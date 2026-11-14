package com.rikkaus.wealth.feedback.api;

import com.rikkaus.wealth.feedback.application.FeedbackService;
import com.rikkaus.wealth.shared.api.ApiPaths;
import com.rikkaus.wealth.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * In-product feedback: send, and list your own.
 *
 * <p>Two routes and no more. There is no route that reads, edits or deletes one entry and no
 * administrative route: the MVP 1D criterion is that feedback is associated with a user and a screen,
 * and the operator reads it from the database. Because no single-entry URI exists, {@code POST}
 * answers 201 with the stored entry in the body and no {@code Location} header, rather than a header
 * pointing at a route that would answer 404.
 *
 * <p>The author is the access token's user, never a value from the request.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/feedback")
class FeedbackController {

    private static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;
    private static final String PROBLEM_SCHEMA_REF = "#/components/schemas/ProblemDetail";

    private final FeedbackService service;

    FeedbackController(FeedbackService service) {
        this.service = service;
    }

    @Operation(
            summary = "List the signed-in user's feedback",
            description =
                    "Returns every piece of feedback the viewer has sent, newest first. Nobody else's "
                            + "feedback is ever included. An empty list is an empty array and a 200.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(
            responseCode = "200",
            description = "The viewer's feedback, possibly empty",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = FeedbackResponse.class)),
                            examples = {
                                @ExampleObject(name = "no feedback yet", value = "[]"),
                                @ExampleObject(
                                        name = "a bug report and an uncategorised wish",
                                        value =
                                                """
                                                [
                                                  {
                                                    "id": "4b7e2c19-8a3d-4f60-9e15-2c8d7a1f5b36",
                                                    "screen": "catalog/assets",
                                                    "category": "BUG",
                                                    "message": "Màn hình danh mục tài sản không hiển thị tỷ giá cập nhật 12/09/2026 sau khi lưu.",
                                                    "createdAt": "2026-11-14T11:52:07.318204Z"
                                                  },
                                                  {
                                                    "id": "d2a91f47-3c6e-4b08-8f5d-71e0b9c4a623",
                                                    "screen": "dashboard",
                                                    "category": null,
                                                    "message": "Mong có biểu đồ dòng tiền theo quý.",
                                                    "createdAt": "2026-11-14T11:47:30.904117Z"
                                                  }
                                                ]
                                                """)
                            }))
    @ApiResponse(
            responseCode = "401",
            description = "No access token, or it has expired or been revoked",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "405",
            description = "Wrong HTTP method",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "406",
            description = "The requested media type cannot be produced",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @GetMapping
    List<FeedbackResponse> listFeedback() {
        return service.listFor(CurrentUser.require()).stream().map(FeedbackResponse::of).toList();
    }

    @Operation(
            summary = "Send feedback from a screen",
            description =
                    "Stores the viewer's feedback against the screen it was sent from. The author is taken "
                            + "from the access token; an id, author or timestamp in the body is ignored. "
                            + "The message is stored as written and returned to its author only; it is "
                            + "never written to an application log.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(
            responseCode = "201",
            description = "The feedback was stored. No Location header: there is no route for one entry.",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FeedbackResponse.class)))
    @ApiResponse(
            responseCode = "400",
            description =
                    "validation-failed when the screen is missing or not a screen code, or the message is "
                            + "missing, blank, longer than 2000 characters or contains U+0000; every problem "
                            + "is reported at once in the errors array. malformed-request when the body "
                            + "cannot be parsed, including an unknown category.",
            content =
                    @Content(
                            mediaType = PROBLEM_JSON,
                            schema = @Schema(ref = PROBLEM_SCHEMA_REF),
                            examples =
                                    @ExampleObject(
                                            name = "a sentence where the screen code belongs, and an empty message",
                                            value =
                                                    """
                                                    {
                                                      "type": "urn:rikkaus:problem:validation-failed",
                                                      "title": "Request validation failed",
                                                      "status": 400,
                                                      "detail": "Invalid request content.",
                                                      "instance": "/api/v1/feedback",
                                                      "correlationId": "8f14e45f-ea8b-4c2d-9a3e-5b7c1d0f2a96",
                                                      "errors": [
                                                        {
                                                          "field": "message",
                                                          "message": "must not be blank"
                                                        },
                                                        {
                                                          "field": "screen",
                                                          "message": "must be a lowercase screen code such as dashboard or catalog/assets"
                                                        }
                                                      ]
                                                    }
                                                    """)))
    @ApiResponse(
            responseCode = "401",
            description = "No access token, or it has expired or been revoked",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "405",
            description = "Wrong HTTP method",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "406",
            description = "The requested media type cannot be produced",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @ApiResponse(
            responseCode = "415",
            description = "The request body is not JSON",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    FeedbackResponse sendFeedback(@Valid @RequestBody FeedbackRequest request) {
        return FeedbackResponse.of(
                service.send(CurrentUser.require(), request.screen(), request.category(), request.message()));
    }
}
