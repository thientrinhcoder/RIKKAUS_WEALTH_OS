/**
 * Generated from services/api/openapi/openapi.json by scripts/generate-api-contract.mjs.
 * Do not edit: run `npm run api:generate` after the contract changes.
 */
export interface paths {
    "/api/v1/settings/exchange-rate": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Read the reporting currency and the declared USD/VND rate
         * @description Returns VND as the fixed reporting currency together with the rate the user declared, the date they declared it effective, and when the server stored that declaration. The three rate fields are null until a rate has been declared; that is the rate-needed state, not an error.
         */
        get: operations["read"];
        /**
         * Declare the USD/VND exchange rate
         * @description Stores the rate and the date the user says it took effect, and stamps the server's own record of when it was stored. The effective date must not be in the future; it is judged as a Vietnamese civil date, not a UTC one.
         */
        put: operations["declare"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/profile": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Read the personal profile
         * @description Returns the signed-in user's identifier, their verified Google email, and the display name they chose — or the Google one if they never changed it.
         */
        get: operations["read_1"];
        /**
         * Change the display name
         * @description Stores the name the user chose. It is kept apart from the name Google reports, so a later sign-in refreshes the Google value without reverting this one. The email is not editable: it comes from the verified identity token.
         */
        put: operations["update"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/liabilities/{liabilityId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Read one liability
         * @description Returns the liability when the signed-in user owns it. An identifier belonging to another user answers not-found with the same body a missing identifier produces.
         */
        get: operations["read_2"];
        /**
         * Replace a liability's editable fields
         * @description Replaces every field except the category, which is chosen once when the liability is created. An omitted optional field is cleared, because this is a replacement rather than a patch: omitting collateralAssetId unlinks the collateral. The same rules as creation apply, and the collateral is re-checked against the signed-in user.
         */
        put: operations["replace"];
        post?: never;
        /**
         * Remove a liability
         * @description Deletes the liability when the signed-in user owns it. A plain removal; undo and a retention policy belong to issue #104. A second delete of the same identifier answers not-found, the same answer any unknown identifier gets. Two deletes that overlap give one 204 and not-found to the rest rather than a server error. The pledged asset, if any, is untouched.
         */
        delete: operations["delete"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/goals/{goalId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Read one goal
         * @description Returns the goal when the signed-in user owns it. An identifier belonging to another user answers not-found with the same body a missing identifier produces, so the response cannot be used to discover which identifiers exist.
         */
        get: operations["read_3"];
        /**
         * Replace a goal
         * @description Replaces every user-entered field of the goal, the type included. An omitted optional field is cleared, because this is a replacement rather than a patch; an omitted allocated amount becomes 0.00. A target date that differs from the stored one must be after today; keeping the stored date is always allowed, so a goal whose date has passed can still be edited.
         */
        put: operations["replace_1"];
        post?: never;
        /**
         * Remove a goal
         * @description Deletes the goal when the signed-in user owns it. Asset, liability and cash-flow records are untouched, as the accepted design's confirmation dialog states. Undo and a retention window belong to issue #104. A second delete of the same identifier answers not-found, the same answer any unknown identifier gets; two deletes that overlap give one 204 and not-found to the rest rather than a server error.
         */
        delete: operations["delete_1"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/cash-flow-entries/{entryId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Read one cash-flow entry
         * @description Returns the entry when the signed-in user owns it. An identifier belonging to another user answers not-found with the same body a missing identifier produces, so the response cannot be used to discover which identifiers exist.
         */
        get: operations["read_4"];
        /**
         * Replace a cash-flow entry
         * @description Replaces every field of the entry with the body, which is the same shape as the create body. An omitted optional member — recurrence, endDate, linkedAssetId, linkedLiabilityId, notes — is cleared, because this is a replacement rather than a patch. Nothing is frozen after creation: the type, category and amount may all change.
         */
        put: operations["replace_2"];
        post?: never;
        /**
         * Remove a cash-flow entry
         * @description Deletes the entry when the signed-in user owns it. This is the plain removal the generic destructive confirmation in the accepted design promises; undo and a retention policy belong to issue #104. A second delete of the same identifier answers not-found, the same answer any unknown identifier gets. Two deletes that overlap, which is what a double-tapped confirm button produces, give one 204 and not-found to the rest rather than a server error.
         */
        delete: operations["delete_2"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/assets/{assetId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Read one asset
         * @description Returns the asset when the signed-in user owns it. An identifier belonging to another user answers not-found with the same body a missing identifier produces, so the response cannot be used to discover which identifiers exist.
         */
        get: operations["read_5"];
        /**
         * Replace an asset's editable metadata
         * @description Replaces the name, liquidity, the optional common fields and the four instrument fields. An omitted optional field is cleared, because this is a replacement rather than a patch, which is how a ticker recorded by mistake is removed. The instrument fields are judged against the asset's stored category, because the category is not editable here. The current value, its currency, the valuation date and the category are not editable here: a new value is recorded through POST /api/v1/assets/{assetId}/valuations and the asset follows it, and the category is chosen once when the asset is created.
         */
        put: operations["replaceMetadata"];
        post?: never;
        /**
         * Remove an asset
         * @description Deletes the asset when the signed-in user owns it. This is the plain removal the generic destructive confirmation in the accepted design promises; undo, linked-record consequences and a retention policy belong to issue #102. A second delete of the same identifier answers not-found, so the response is not idempotent and that is deliberate: it is the same answer any unknown identifier gets. Two deletes that overlap, which is what a double-tapped confirm button produces, give one 204 and not-found to the rest rather than a server error.
         */
        delete: operations["delete_3"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/liabilities": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * List the signed-in user's liabilities
         * @description Returns every liability the viewer owns, soonest next payment first, those with no next payment date after them, and then by name. An empty catalog is an empty array and a 200, not a 404. Amounts are reported in each liability's own currency; converting a USD balance to VND is the client's, using the rate from GET /api/v1/settings/exchange-rate. The optional q, category, currency and sort parameters narrow and reorder the catalog; without them the response is the whole catalog in the default order.
         */
        get: operations["list"];
        put?: never;
        /**
         * Add a liability to the catalog
         * @description Creates a liability owned by the signed-in user. Terms and schedule are recorded as stated and nothing is computed from them. The maturity date must not be before the start date, the next payment date must fall inside the term when the term is given, the recurring amount and frequency come as a pair, and a collateral must be an asset the signed-in user owns.
         */
        post: operations["create"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/goals": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * List the signed-in user's goals
         * @description Returns every goal the viewer owns, nearest target date first and then by name. An empty list is an empty array and a 200, not a 404. Every amount is in VND. No projection is included: completion, remaining amount, remaining time, required contribution and status arrive with issue #89. The optional q, type and sort parameters narrow and reorder the list; without them the response is every goal in the default order.
         */
        get: operations["list_1"];
        put?: never;
        /**
         * Create a goal
         * @description Creates a goal owned by the signed-in user from any of the seven templates. The target amount must be greater than zero, the allocated amount may not exceed it, and the target date must be after today as a Vietnamese civil date. The expected return is the user's own assumption, 0 to 30 percent, and is informational only.
         */
        post: operations["create_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/feedback": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * List the signed-in user's feedback
         * @description Returns every piece of feedback the viewer has sent, newest first. Nobody else's feedback is ever included. An empty list is an empty array and a 200.
         */
        get: operations["listFeedback"];
        put?: never;
        /**
         * Send feedback from a screen
         * @description Stores the viewer's feedback against the screen it was sent from. The author is taken from the access token; an id, author or timestamp in the body is ignored. The message is stored as written and returned to its author only; it is never written to an application log.
         */
        post: operations["sendFeedback"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/cash-flow-entries": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * List the signed-in user's cash-flow entries
         * @description Returns every income and expense entry the viewer owns, newest effective date first and then newest created first. An empty catalog is an empty array and a 200, not a 404. Amounts are reported in each entry's own currency; converting a USD entry to VND is the client's, using the rate from GET /api/v1/settings/exchange-rate. A recurring entry is one row, not one row per month: the dates it falls on are GET /api/v1/cash-flow-occurrences. The optional q, entryType, category, timing, recurrence, currency, effectiveFrom, effectiveTo and sort parameters narrow and reorder the catalog; without them the response is the whole catalog in the default order.
         */
        get: operations["list_2"];
        put?: never;
        /**
         * Add a cash-flow entry
         * @description Creates an income or expense entry owned by the signed-in user. The category must belong to the entry type. A one-time entry has an effective date and nothing else; a recurring one has a start date, a recurrence and an optional end date. Dates may be in the future: an expected entry is a legitimate one. An entry may link to one of the viewer's assets or one of their liabilities, never both; a linked record that is not the viewer's, or that does not exist, is refused with the same message either way.
         */
        post: operations["create_2"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/auth/refresh": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Renew a session
         * @description Exchanges a refresh token for a new access token and a new refresh token. The presented token is single-use: presenting it again is treated as evidence that it leaked and revokes the whole session. A native client sends the token in the body. The web client sends no body token, the `rikkaus_refresh` cookie, and `X-Rikkaus-Client: web`; the rotated token comes back as a new cookie.
         */
        post: operations["renew"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/auth/logout": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Sign out
         * @description Revokes the session the refresh token belongs to. Returns 204 whether or not the token was still live, so the endpoint cannot be used to test whether a token is valid. The token comes from the body, or for the web client from the `rikkaus_refresh` cookie with `X-Rikkaus-Client: web`. Every 204 also expires the cookie.
         */
        post: operations["signOut"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/auth/google": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Complete a Google sign-in and start a session
         * @description Exchanges an authorization code with Google using PKCE, creates the account on first sign-in, and returns an access token with a rotating refresh token. Google is the only identity provider; there is no password.
         */
        post: operations["signInWithGoogle"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/assets": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * List the signed-in user's assets
         * @description Returns every asset the viewer owns, newest valuation date first and then by name, so the catalog's order is the server's rather than the database's incidental one. An empty catalog is an empty array and a 200, not a 404. Values are reported in each asset's own currency; converting a USD holding to VND is the client's, using the rate from GET /api/v1/settings/exchange-rate. The optional q, category, currency, liquidity and sort parameters narrow and reorder the catalog; without them the response is the whole catalog in the default order.
         */
        get: operations["list_3"];
        put?: never;
        /**
         * Add an asset to the catalog
         * @description Creates an asset owned by the signed-in user, and the first record of its valuation history with the same value, currency, date and optional rate snapshot. All prices are manually entered; there is no market-price integration in MVP 1A. The valuation date must not be in the future and a purchase date must not be later than it, both judged as Vietnamese civil dates. A later value is recorded through POST /api/v1/assets/{assetId}/valuations, never by editing this one. The four instrument fields are optional everywhere they are accepted, and sending none of them is valid for every category. A symbol, a cost per unit or a current price per unit is accepted only for LISTED_SECURITIES, BOND, INVESTMENT_FUND and OTHER_ASSET; an interest rate only for BOND, TERM_DEPOSIT and OTHER_ASSET. currentValue is never derived from a quantity and a unit price, and a request where their product differs from it is stored as sent.
         */
        post: operations["create_3"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/assets/{assetId}/valuations": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * List an asset's valuation history
         * @description Returns every valuation ever recorded for the asset, newest first: latest effective date, and between two records on one date the later recorded. Exactly one record is marked current, and it is the one the asset's currentValue, currency and valuationDate are taken from. Values are in the asset's own currency with the rate the user applied; converting to VND is the client's. An asset always has at least one record, written when it was created.
         */
        get: operations["list_4"];
        put?: never;
        /**
         * Record a valuation
         * @description Appends a record to the asset's history. Nothing already recorded changes. When the new record's effective date is on or after the current record's, the asset's currentValue, currency and valuationDate become this record's in the same transaction and the response marks it current; a record dated earlier is kept as history and marks nothing. Two records on one date are a correction and its original, and the later recorded is current. To correct an earlier record, send its identifier in correctsValuationId with the reason in note; the original stays readable. The currency must be the asset's. A USD record may carry the USD/VND rate the user applied and its date, both or neither; a VND record carries neither.
         */
        post: operations["record"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/onboarding": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Read the guided-onboarding checklist
         * @description Returns the seven onboarding steps in backbone order, each done or not according to the user's own records at this moment, together with the step the user last viewed and whether they completed or dismissed the flow. A user who has recorded no progress reads as step 1, in progress; reading never creates progress.
         */
        get: operations["read_6"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        /**
         * Record progress through the guided onboarding
         * @description Records any of: the step the user is viewing, that they reviewed their first dashboard, and that they completed, dismissed or reopened the flow. Members left out or null are unchanged; an empty body changes nothing and returns the checklist. Whether the other six steps are done cannot be written: it is read from the user's records.
         */
        patch: operations["recordProgress"];
        trace?: never;
    };
    "/api/v1/meta": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Report which build of the API the caller is talking to
         * @description Unauthenticated by design. Exposes the application name, the API version, the build version and the current server time in UTC, and nothing else. The build timestamp is deliberately withheld.
         */
        get: operations["meta"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/goal-templates": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * List the seven goal templates
         * @description Returns every template with the defaults the accepted design suggests for a new goal of that type: an expected annual return, a horizon in months, and which amount the form should prefill from figures the client already holds. Every value is a suggestion the user may change. The list is fixed and identical for every user.
         */
        get: operations["list_5"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/cash-flow-reports/monthly": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Report the signed-in user's cash flow for one calendar month
         * @description Expands the viewer's entries into the month exactly as GET /api/v1/cash-flow-occurrences does for the window from the first to the last day of the month, and sums those occurrences. Every amount is in VND. A USD entry is converted with the rate the user has declared, one occurrence at a time, rounded half-up to whole dong; the rate and its effective date are returned in exchangeRate. Without a declared rate a USD occurrence keeps its originalAmount, its amount is null, it is left out of every total, complete is false and unconverted says how much was left out. net is income minus expense. Each composition lists the categories present, largest first, with shares apportioned by largest remainder so they sum to exactly 100.0. Passive income is income linked to an asset, grouped by asset; income in a passive-kind category with no link is reported as unassigned and not counted as passive. Every total equals the sum of the occurrences returned. A month with no occurrence is the same structure with zeros and empty arrays, and a share that cannot be computed is null. Past, current and future months are answered alike; nothing is compared with today and nothing is stored.
         */
        get: operations["monthly"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/cash-flow-occurrences": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * List the occurrences of the signed-in user's cash-flow entries in a window
         * @description Expands every one-time and recurring entry the viewer owns into the dates it falls on from `from` to `to`, both included. A recurring entry's first occurrence is its effective date and its last is on or before its end date, both included. A day of month the month does not have moves back to that month's last day and is marked shiftedToMonthEnd; the next month returns to the entry's own day. Each occurrence belongs to the month containing its date, so two windows that meet without sharing a day count every occurrence exactly once. Dates are calendar dates with no time or zone, so the answer does not depend on when or where it is asked. Occurrences are computed on every read and never stored; editing or deleting an entry changes its occurrences immediately. Ordered by date, then by sourceEntryId. An empty window is an empty array.
         */
        get: operations["list_6"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/cash-flow-forecast": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Project the signed-in user's cash over three whole months
         * @description Three calendar months starting at `from`, or at the month after today in Asia/Ho_Chi_Minh when it is absent. Only records the user can see contribute: the occurrences of their one-time and recurring cash-flow entries, expanded with the rules of /cash-flow-occurrences; the payments of their liabilities, counted from nextPaymentDate every recurringPaymentFrequency and never after maturityDate; and their CASH and BANK_ACCOUNT assets as the opening balance, applied at the start of the first day. A liability with a recurring entry linked to it that overlaps the horizon is counted through that entry only. USD amounts are converted with the user's declared rate and each amount is rounded to whole đồng before it is added, so a month's items add up to its totals exactly. The lowest position is judged on the opening balance and at the end of every day an amount lands. Every record that could have contributed and does not is listed in excludedSources with its reason; without a declared rate, USD records are excluded and the months they fall in are partial rather than the request failing. The days between today and the horizon are not projected and are named in assumptions.notProjected. Computed on every read and never stored. The same records, day and `from` give the same bytes.
         */
        get: operations["forecast"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/auth/session": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Read the signed-in user
         * @description Confirms the access token is still valid and returns who it belongs to. This is how a client tells a live session from an expired or revoked one without having to infer it from a failed domain request.
         */
        get: operations["currentSession"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/api/v1/assets/{assetId}/valuations/{valuationId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Read one valuation record
         * @description Returns the record when the signed-in user owns the asset and the record belongs to it. An asset of another user, or a record of any other asset, answers not-found with the same body a missing identifier produces.
         */
        get: operations["read_7"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
}
export type webhooks = Record<string, never>;
export interface components {
    schemas: {
        /** @description RFC 9457 problem details. Returned for every error under /api/v1/**. */
        ProblemDetail: {
            /**
             * @description Stable taxonomy identifier. A URN rather than a URL because the project owns no domain; RFC 9457 does not require it to dereference.
             * @enum {string}
             */
            type: "urn:rikkaus:problem:forbidden" | "urn:rikkaus:problem:internal-error" | "urn:rikkaus:problem:malformed-request" | "urn:rikkaus:problem:method-not-allowed" | "urn:rikkaus:problem:not-acceptable" | "urn:rikkaus:problem:not-found" | "urn:rikkaus:problem:unauthorized" | "urn:rikkaus:problem:unsupported-media-type" | "urn:rikkaus:problem:validation-failed";
            /** @description Short, human-readable summary. */
            title: string;
            /**
             * Format: int32
             * @description The HTTP status code.
             */
            status: number;
            /** @description Human-readable explanation, safe to display. Never contains a stack trace, an internal class name or a credential. */
            detail?: string;
            /** @description The request URI the problem occurred for. */
            instance?: string;
            /** @description Extension member. The same value as the X-Correlation-Id response header and the server's log lines for this request. */
            correlationId?: string;
            /** @description Extension member, present on validation failures only. One entry per rejected field. */
            errors?: {
                field?: string;
                message?: string;
            }[];
        };
        /** @description Declare the USD/VND exchange rate and the date it took effect. */
        DeclareExchangeRateRequest: {
            /**
             * @description How many VND one USD buys. Must be greater than zero.
             * @example 25100
             */
            usdToVndRate: number;
            /**
             * Format: date
             * @description The date you confirm this rate took effect. Must not be in the future.
             * @example 2026-09-12
             */
            effectiveDate: string;
        };
        /** @description The reporting currency and the user-declared USD/VND exchange rate. */
        ExchangeRateResponse: {
            /**
             * @description The reporting currency. Fixed to VND for MVP 1A.
             * @example VND
             */
            baseCurrency?: string;
            /**
             * @description How many VND one USD buys, as declared by the user. Null until declared.
             * @example 25100
             */
            usdToVndRate?: number;
            /**
             * Format: date
             * @description The date the user declares the rate took effect. Not the time it was stored.
             * @example 2026-09-12
             */
            effectiveDate?: string;
            /**
             * Format: date-time
             * @description When the server stored the declaration.
             * @example 2026-09-12T02:42:00Z
             */
            updatedAt?: string;
        };
        /** @description Change the display name. The only editable profile field. */
        UpdateProfileRequest: {
            /**
             * @description The name to show. Trimmed before storing.
             * @example An Nguyễn
             */
            displayName: string;
        };
        /** @description The signed-in user's personal profile. */
        ProfileResponse: {
            /**
             * Format: uuid
             * @example 0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93
             */
            id?: string;
            /**
             * @description From the verified Google identity token. Read-only here.
             * @example an.nguyen@example.com
             */
            email?: string;
            /**
             * @description The name the user chose, or the Google name if they never chose one.
             * @example An Nguyễn
             */
            displayName?: string;
        };
        /** @description Replace a liability's editable fields. The category is not editable. */
        UpdateLiabilityRequest: {
            /**
             * @description What the user calls this liability.
             * @example Vay mua nhà Vietcombank
             */
            name: string;
            /**
             * @description What is still owed, as the user now states it. Zero is allowed, negative is not.
             * @example 2376500000
             */
            outstandingBalance: number;
            /**
             * @description The currency the balance and the recurring payment are expressed in.
             * @example VND
             * @enum {string}
             */
            currency: "VND" | "USD";
            /**
             * @description Nominal annual interest rate in percent. Omit to clear it.
             * @example 8.5
             */
            interestRate?: number;
            /**
             * Format: date
             * @description When the liability began. Omit to clear it.
             * @example 2024-03-15
             */
            startDate?: string;
            /**
             * Format: date
             * @description When it is due to be settled. Must not be before the start date. Omit to clear it.
             * @example 2044-03-15
             */
            maturityDate?: string;
            /**
             * @description The total due each period. Must be greater than zero and come with a frequency. Omit to clear the schedule.
             * @example 23500000
             */
            recurringPaymentAmount?: number;
            /**
             * @description How often the recurring payment falls due. Must come with an amount.
             * @example MONTHLY
             * @enum {string}
             */
            recurringPaymentFrequency?: "MONTHLY" | "QUARTERLY" | "SEMIANNUAL" | "YEARLY";
            /**
             * Format: date
             * @description The next date a payment is due, inside the term when the term is given. Omit to clear it.
             * @example 2027-01-05
             */
            nextPaymentDate?: string;
            /**
             * Format: uuid
             * @description The asset pledged against this liability; must be the signed-in user's. Omit to unlink.
             * @example a81d4f27-6c3e-4b19-8f55-0d2e7c9a3b64
             */
            collateralAssetId?: string;
            /**
             * @description Anything the user wants to remember. Omit to clear it.
             * @example Đã trả trước 23.500.000 ₫ tháng 12.
             */
            notes?: string;
        };
        /** @description A liability in the signed-in user's catalog. */
        LiabilityResponse: {
            /**
             * Format: uuid
             * @description The liability's identifier.
             * @example 5c0e2b7a-91d4-4f3e-8a6b-7d2c1e9f4a30
             */
            id?: string;
            /**
             * @description What the user calls this liability.
             * @example Vay mua nhà Vietcombank
             */
            name?: string;
            /**
             * @description One of the six liability categories.
             * @example BANK_LOAN
             * @enum {string}
             */
            category?: "BANK_LOAN" | "PERSONAL_LOAN" | "MARGIN_LOAN" | "CREDIT_CARD_BALANCE" | "GUARANTEE_OBLIGATION" | "OTHER_LIABILITY";
            /**
             * @description What is still owed, in its own currency.
             * @example 2400000000
             */
            outstandingBalance?: number;
            /**
             * @description The currency the amounts are expressed in.
             * @example VND
             * @enum {string}
             */
            currency?: "VND" | "USD";
            /**
             * @description Nominal annual rate in percent. Null when not recorded.
             * @example 8.5
             */
            interestRate?: number;
            /**
             * Format: date
             * @description When the liability began. Null when not recorded.
             * @example 2024-03-15
             */
            startDate?: string;
            /**
             * Format: date
             * @description When it is due to be settled. Null when not recorded.
             * @example 2044-03-15
             */
            maturityDate?: string;
            /**
             * @description The total due each period. Null when there is no recurring payment.
             * @example 23500000
             */
            recurringPaymentAmount?: number;
            /**
             * @description How often it falls due. Null when there is no recurring payment.
             * @example MONTHLY
             * @enum {string}
             */
            recurringPaymentFrequency?: "MONTHLY" | "QUARTERLY" | "SEMIANNUAL" | "YEARLY";
            /**
             * Format: date
             * @description The next date a payment is due. Null when not recorded.
             * @example 2026-12-05
             */
            nextPaymentDate?: string;
            /**
             * Format: uuid
             * @description The pledged asset, owned by the same user. Null when none, or when that asset was deleted.
             * @example a81d4f27-6c3e-4b19-8f55-0d2e7c9a3b64
             */
            collateralAssetId?: string;
            /**
             * @description The user's own note. Null when not recorded.
             * @example Thế chấp căn hộ Thảo Điền.
             */
            notes?: string;
            /**
             * Format: date-time
             * @description When the server first stored this liability.
             * @example 2026-11-14T10:42:11.503Z
             */
            createdAt?: string;
            /**
             * Format: date-time
             * @description When the server last stored a change to it.
             * @example 2026-11-14T10:42:11.503Z
             */
            updatedAt?: string;
        };
        /** @description Create or replace a goal. The same fields for every template. */
        GoalRequest: {
            /**
             * @description What the user calls this goal.
             * @example Mua căn hộ
             */
            name: string;
            /**
             * @description Which of the seven templates this goal follows. Editable.
             * @example HOME_PURCHASE
             * @enum {string}
             */
            type: "EMERGENCY_FUND" | "HOME_PURCHASE" | "EDUCATION" | "RETIREMENT" | "NET_WORTH_GROWTH" | "DEBT_REDUCTION" | "OTHER_GOAL";
            /**
             * @description The amount to reach, in VND. Must be greater than zero.
             * @example 3500000000
             */
            targetAmount: number;
            /**
             * @description What has already been set aside for this goal, in VND. Zero or more, and never above the target amount. Omit for nothing set aside yet, which is stored as 0.00.
             * @example 1200000000
             */
            currentAllocatedAmount?: number;
            /**
             * Format: date
             * @description The date to reach the goal by. When created, or when changed, it must be after today as a Vietnamese civil date; keeping a stored date is always allowed.
             * @example 2030-12-31
             */
            targetDate: string;
            /**
             * @description What the user plans to add each month, in VND. Zero or more. Omit when not stated; that is kept distinct from zero.
             * @example 25000000
             */
            plannedMonthlyContribution?: number;
            /**
             * @description The user's own assumption for the annual return, in percent, from 0 to 30. Omit for no assumption. Informational only; never a promise.
             * @example 7
             */
            expectedAnnualReturnPercent?: number;
        };
        /** @description A financial goal owned by the signed-in user. Inputs only; projections are issue #89. */
        GoalResponse: {
            /**
             * Format: uuid
             * @description The goal's identifier.
             * @example 5c2e7d31-9a4f-4b8e-a1d6-3f7b2c9e8a40
             */
            id?: string;
            /**
             * @description What the user calls this goal.
             * @example Mua căn hộ
             */
            name?: string;
            /**
             * @description Which of the seven templates this goal follows.
             * @example HOME_PURCHASE
             * @enum {string}
             */
            type?: "EMERGENCY_FUND" | "HOME_PURCHASE" | "EDUCATION" | "RETIREMENT" | "NET_WORTH_GROWTH" | "DEBT_REDUCTION" | "OTHER_GOAL";
            /**
             * @description The currency every amount is in. Always VND.
             * @example VND
             * @enum {string}
             */
            currency?: "VND" | "USD";
            /**
             * @description The amount to reach.
             * @example 3500000000
             */
            targetAmount?: number;
            /**
             * @description What has been set aside so far. 0.00 when nothing yet.
             * @example 1200000000
             */
            currentAllocatedAmount?: number;
            /**
             * Format: date
             * @description The date to reach the goal by.
             * @example 2030-12-31
             */
            targetDate?: string;
            /**
             * @description Planned monthly contribution. Null when not stated.
             * @example 25000000
             */
            plannedMonthlyContribution?: number;
            /**
             * @description The user's assumed annual return in percent. Null when no assumption.
             * @example 7
             */
            expectedAnnualReturnPercent?: number;
            /**
             * Format: date-time
             * @description When the server first stored this goal.
             * @example 2026-11-14T10:35:40.512Z
             */
            createdAt?: string;
            /**
             * Format: date-time
             * @description When the server last stored a change to it.
             * @example 2026-11-14T10:35:40.512Z
             */
            updatedAt?: string;
        };
        /** @description Create or replace a cash-flow entry. The same body serves both. */
        CashFlowEntryRequest: {
            /**
             * @description Whether the money comes in or goes out. Decides the sign; the amount is always positive.
             * @example INCOME
             * @enum {string}
             */
            entryType: "INCOME" | "EXPENSE";
            /**
             * @description One of the seven income or nine expense categories. Must belong to the entry type given.
             * @example SALARY
             * @enum {string}
             */
            category: "SALARY" | "BUSINESS_INCOME" | "RENTAL_INCOME" | "DIVIDEND" | "DEPOSIT_INTEREST" | "BOND_OR_FUND_INCOME" | "OTHER_INCOME" | "LIVING_COSTS" | "HOUSING" | "EDUCATION" | "HEALTHCARE" | "INSURANCE" | "TAX_AND_FEES" | "DEBT_PRINCIPAL_PAYMENT" | "INTEREST_PAYMENT" | "OTHER_EXPENSE";
            /**
             * @description The amount, in this entry's currency. Greater than zero.
             * @example 45000000
             */
            amount: number;
            /**
             * @description The currency the amount is expressed in. No conversion is stored; the client converts with the rate from the settings resource.
             * @example VND
             * @enum {string}
             */
            currency: "VND" | "USD";
            /**
             * @description ONE_TIME or RECURRING. Decides whether recurrence and endDate may be present.
             * @example RECURRING
             * @enum {string}
             */
            timing: "ONE_TIME" | "RECURRING";
            /**
             * @description How often a recurring entry repeats. Required when timing is RECURRING and must be omitted when it is ONE_TIME.
             * @example MONTHLY
             * @enum {string}
             */
            recurrence?: "MONTHLY" | "QUARTERLY" | "YEARLY";
            /**
             * Format: date
             * @description The date a one-time entry applies, or the first date of a recurring one. May be in the future.
             * @example 2026-01-01
             */
            effectiveDate: string;
            /**
             * Format: date
             * @description The last date a recurring entry applies. Only for RECURRING; omit for open-ended. Must not be before effectiveDate.
             * @example 2029-06-30
             */
            endDate?: string;
            /**
             * Format: uuid
             * @description An asset of the signed-in user this entry refers to, such as the apartment a rent comes from. Omit to leave it unlinked, or to clear it on PUT.
             * @example a81d4f27-6c3e-4b19-8f55-0d2e7c9a3b64
             */
            linkedAssetId?: string;
            /**
             * Format: uuid
             * @description A liability of the signed-in user this entry refers to, such as the mortgage an interest payment belongs to. Must not be sent together with linkedAssetId. Omit to leave it unlinked, or to clear it on PUT.
             * @example 7d3e9a14-2c6b-4f81-a0d5-6b8e1f4c2a97
             */
            linkedLiabilityId?: string;
            /**
             * @description Anything the user wants to remember about it. Omit to clear it on PUT.
             * @example Lương tháng
             */
            notes?: string;
        };
        /** @description A cash-flow entry in the signed-in user's catalog. */
        CashFlowEntryResponse: {
            /**
             * Format: uuid
             * @description The entry's identifier.
             * @example 5c0e2b7a-91d4-4f3b-8a6e-2d7f1c9b4e30
             */
            id?: string;
            /**
             * @description INCOME or EXPENSE.
             * @example INCOME
             * @enum {string}
             */
            entryType?: "INCOME" | "EXPENSE";
            /**
             * @description One of the sixteen categories, always of the entry's type.
             * @example SALARY
             * @enum {string}
             */
            category?: "SALARY" | "BUSINESS_INCOME" | "RENTAL_INCOME" | "DIVIDEND" | "DEPOSIT_INTEREST" | "BOND_OR_FUND_INCOME" | "OTHER_INCOME" | "LIVING_COSTS" | "HOUSING" | "EDUCATION" | "HEALTHCARE" | "INSURANCE" | "TAX_AND_FEES" | "DEBT_PRINCIPAL_PAYMENT" | "INTEREST_PAYMENT" | "OTHER_EXPENSE";
            /**
             * @description The amount, in the entry's own currency. Always positive.
             * @example 45000000
             */
            amount?: number;
            /**
             * @description The currency the amount is expressed in.
             * @example VND
             * @enum {string}
             */
            currency?: "VND" | "USD";
            /**
             * @description ONE_TIME or RECURRING.
             * @example RECURRING
             * @enum {string}
             */
            timing?: "ONE_TIME" | "RECURRING";
            /**
             * @description MONTHLY, QUARTERLY or YEARLY for a recurring entry. Null for a one-time one.
             * @example MONTHLY
             * @enum {string}
             */
            recurrence?: "MONTHLY" | "QUARTERLY" | "YEARLY";
            /**
             * Format: date
             * @description The date a one-time entry applies, or the first date of a recurring one.
             * @example 2026-01-01
             */
            effectiveDate?: string;
            /**
             * Format: date
             * @description The last date a recurring entry applies. Null when open-ended or one-time.
             * @example 2029-06-30
             */
            endDate?: string;
            /**
             * Format: uuid
             * @description The user's asset this entry refers to. Null when unlinked.
             * @example a81d4f27-6c3e-4b19-8f55-0d2e7c9a3b64
             */
            linkedAssetId?: string;
            /**
             * Format: uuid
             * @description The user's liability this entry refers to. Null when unlinked.
             * @example 7d3e9a14-2c6b-4f81-a0d5-6b8e1f4c2a97
             */
            linkedLiabilityId?: string;
            /**
             * @description The user's own note. Null when not recorded.
             * @example Lương tháng
             */
            notes?: string;
            /**
             * Format: date-time
             * @description When the server first stored this entry.
             * @example 2026-11-14T10:42:11.503Z
             */
            createdAt?: string;
            /**
             * Format: date-time
             * @description When the server last stored a change to it.
             * @example 2026-11-14T10:42:11.503Z
             */
            updatedAt?: string;
        };
        /** @description Replace an asset's editable metadata. The valuation is not editable here. */
        UpdateAssetMetadataRequest: {
            /**
             * @description What the user calls this asset.
             * @example Tài khoản USD dự phòng
             */
            name: string;
            /**
             * @description How readily the user judges this asset can be turned into cash.
             * @example HIGH
             * @enum {string}
             */
            liquidity: "HIGH" | "MEDIUM" | "LOW";
            /**
             * @description Purchase value or cost basis, in the asset's own currency. Omit to clear it.
             * @example 19500
             */
            purchaseValue?: number;
            /**
             * Format: date
             * @description When the asset was acquired. Must not be in the future and must not be later than the asset's stored valuation date. Omit to clear it.
             * @example 2024-03-18
             */
            purchaseDate?: string;
            /**
             * @description Units held. Must be greater than zero when present. Omit to clear it.
             * @example 20000
             */
            quantity?: number;
            /**
             * @description Where the asset is held or stored. Omit to clear it.
             * @example Ngân hàng An Tín
             */
            institution?: string;
            /**
             * @description Anything the user wants to remember. Omit to clear it.
             * @example Đã chuyển sang kỳ hạn 6 tháng.
             */
            notes?: string;
            /**
             * @description Ticker, ISIN or fund code, as the user types it. Nothing resolves it against a market. Omit to clear it. Accepted for LISTED_SECURITIES, BOND, INVESTMENT_FUND and OTHER_ASSET; any other category refuses it.
             * @example VCB
             */
            symbol?: string;
            /**
             * @description What one unit cost, in this asset's own currency. Entered by the user and never multiplied by the quantity. Accepted for the same four categories as the symbol.
             * @example 21800
             */
            costPerUnit?: number;
            /**
             * @description What one unit is worth now, in this asset's own currency. Entered by the user; currentValue is a separate field and is never derived from this one. Accepted for the same four categories as the symbol.
             * @example 24150
             */
            currentPricePerUnit?: number;
            /**
             * @description Interest rate or expected yield as a percent per year, so 6.2500 is 6.25%. No interest is accrued and no yield projected from it. Accepted for BOND, TERM_DEPOSIT and OTHER_ASSET only: a listed security has no rate, and a fund's expected return belongs in the notes.
             * @example 6.25
             */
            interestRate?: number;
        };
        /** @description An asset in the signed-in user's catalog. */
        AssetResponse: {
            /**
             * Format: uuid
             * @description The asset's identifier.
             * @example 3f1c9a52-8d4b-4f0e-9c21-5b7a8e6d4f10
             */
            id?: string;
            /**
             * @description What the user calls this asset.
             * @example Tài khoản USD
             */
            name?: string;
            /**
             * @description One of the eleven asset categories.
             * @example BANK_ACCOUNT
             * @enum {string}
             */
            category?: "CASH" | "BANK_ACCOUNT" | "TERM_DEPOSIT" | "LISTED_SECURITIES" | "BOND" | "INVESTMENT_FUND" | "REAL_ESTATE" | "PRIVATE_BUSINESS_EQUITY" | "VEHICLE_OR_VALUABLE_ASSET" | "COLLECTIBLE" | "OTHER_ASSET";
            /**
             * @description What the asset is worth, in its own currency: the value of the current record in its valuation history.
             * @example 20000
             */
            currentValue?: number;
            /**
             * @description The currency the values are expressed in.
             * @example USD
             * @enum {string}
             */
            currency?: "VND" | "USD";
            /**
             * Format: date
             * @description The date the user states the value was accurate: the effective date of the current valuation record.
             * @example 2026-09-12
             */
            valuationDate?: string;
            /**
             * @description How readily the user judges it can be turned into cash.
             * @example HIGH
             * @enum {string}
             */
            liquidity?: "HIGH" | "MEDIUM" | "LOW";
            /**
             * @description Purchase value or cost basis. Null when not recorded.
             * @example 19500
             */
            purchaseValue?: number;
            /**
             * Format: date
             * @description When the asset was acquired. Null when not recorded.
             * @example 2024-03-18
             */
            purchaseDate?: string;
            /**
             * @description Units held. Null when not recorded.
             * @example 20000
             */
            quantity?: number;
            /**
             * @description Where it is held or stored. Null when not recorded.
             * @example Ngân hàng An Tín
             */
            institution?: string;
            /**
             * @description The user's own note. Null when not recorded.
             * @example Tiền gửi dự phòng bằng USD.
             */
            notes?: string;
            /**
             * @description Ticker, ISIN or fund code. Null when not recorded.
             * @example VCB
             */
            symbol?: string;
            /**
             * @description What one unit cost. Null when not recorded.
             * @example 21800
             */
            costPerUnit?: number;
            /**
             * @description What one unit is worth now. Null when not recorded. currentValue is not derived from it.
             * @example 24150
             */
            currentPricePerUnit?: number;
            /**
             * @description Interest rate or expected yield as a percent per year, so 6.2500 is 6.25%. Null when not recorded.
             * @example 6.25
             */
            interestRate?: number;
            /**
             * Format: date-time
             * @description When the server first stored this asset.
             * @example 2026-09-12T02:42:11.503Z
             */
            createdAt?: string;
            /**
             * Format: date-time
             * @description When the server last stored a change to it.
             * @example 2026-09-12T02:42:11.503Z
             */
            updatedAt?: string;
        };
        /** @description Create a liability. The category is chosen here and cannot be changed later. */
        CreateLiabilityRequest: {
            /**
             * @description What the user calls this liability.
             * @example Vay mua nhà Vietcombank
             */
            name: string;
            /**
             * @description One of the six liability categories.
             * @example BANK_LOAN
             * @enum {string}
             */
            category: "BANK_LOAN" | "PERSONAL_LOAN" | "MARGIN_LOAN" | "CREDIT_CARD_BALANCE" | "GUARANTEE_OBLIGATION" | "OTHER_LIABILITY";
            /**
             * @description What is still owed, in this liability's currency, as the user states it. Zero is allowed, negative is not.
             * @example 2400000000
             */
            outstandingBalance: number;
            /**
             * @description The currency the balance and the recurring payment are expressed in. No conversion happens here; the client holds the declared rate.
             * @example VND
             * @enum {string}
             */
            currency: "VND" | "USD";
            /**
             * @description Nominal annual interest rate in percent, as the user states it: 8.5 means 8.5 % per year. Recorded only; nothing accrues from it.
             * @example 8.5
             */
            interestRate?: number;
            /**
             * Format: date
             * @description When the liability began.
             * @example 2024-03-15
             */
            startDate?: string;
            /**
             * Format: date
             * @description When it is due to be settled. Must not be before the start date.
             * @example 2044-03-15
             */
            maturityDate?: string;
            /**
             * @description The total due each period, in the liability's currency. Must be greater than zero, and must come with a frequency.
             * @example 23500000
             */
            recurringPaymentAmount?: number;
            /**
             * @description How often the recurring payment falls due. Must come with an amount.
             * @example MONTHLY
             * @enum {string}
             */
            recurringPaymentFrequency?: "MONTHLY" | "QUARTERLY" | "SEMIANNUAL" | "YEARLY";
            /**
             * Format: date
             * @description The next date a payment is due. Must fall between the start and maturity dates when those are given. May be in the past.
             * @example 2026-12-05
             */
            nextPaymentDate?: string;
            /**
             * Format: uuid
             * @description The asset pledged against this liability. Must identify an asset the signed-in user owns; any other identifier is refused with the field named.
             * @example a81d4f27-6c3e-4b19-8f55-0d2e7c9a3b64
             */
            collateralAssetId?: string;
            /**
             * @description Anything the user wants to remember about it.
             * @example Thế chấp căn hộ Thảo Điền.
             */
            notes?: string;
        };
        /** @description Feedback sent from one screen of the app. */
        FeedbackRequest: {
            /**
             * @description The screen the feedback was sent from, as a route-like code: lowercase ASCII letters and digits in segments joined by '-' or '/', at most 64 characters.
             * @example catalog/assets
             */
            screen: string;
            /**
             * @description What kind of feedback this is. Omit when the user did not choose one.
             * @example BUG
             * @enum {string}
             */
            category?: "BUG" | "IDEA" | "QUESTION" | "OTHER";
            /**
             * @description The user's own words. Surrounding blank space is removed; must not be blank, at most 2000 characters, and must not contain U+0000.
             * @example Màn hình danh mục tài sản không hiển thị tỷ giá cập nhật 12/09/2026 sau khi lưu.
             */
            message: string;
        };
        /** @description Feedback the signed-in user sent. */
        FeedbackResponse: {
            /**
             * Format: uuid
             * @description The feedback's identifier.
             * @example 4b7e2c19-8a3d-4f60-9e15-2c8d7a1f5b36
             */
            id?: string;
            /**
             * @description The screen code it was sent from.
             * @example catalog/assets
             */
            screen?: string;
            /**
             * @description BUG, IDEA, QUESTION or OTHER; null when the user did not choose one.
             * @example BUG
             * @enum {string}
             */
            category?: "BUG" | "IDEA" | "QUESTION" | "OTHER";
            /**
             * @description The user's own words, as stored.
             * @example Màn hình danh mục tài sản không hiển thị tỷ giá cập nhật 12/09/2026 sau khi lưu.
             */
            message?: string;
            /**
             * Format: date-time
             * @description When the server stored it.
             * @example 2026-11-14T11:52:07.318204Z
             */
            createdAt?: string;
        };
        /** @description Carries the refresh token identifying the session to renew or end. */
        RefreshSessionRequest: {
            /**
             * @description The refresh token returned by the most recent sign-in or renewal. Required for native clients. The web client omits it (or the whole body) and sends the `rikkaus_refresh` cookie with `X-Rikkaus-Client: web` instead.
             * @example 1mQ7rVx0Yb3TkPzFhLdW8sNcJgA6eRuq2ZoXiB4ySvE
             */
            refreshToken?: string;
        };
        /** @description The signed-in user. */
        AuthenticatedUserResponse: {
            /**
             * Format: uuid
             * @example 0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93
             */
            id?: string;
            /** @example an.nguyen@example.com */
            email?: string;
            /** @example An Nguyễn */
            displayName?: string;
        };
        /** @description A signed-in session: an access token, its lifetime, and the refresh token. */
        SessionResponse: {
            /**
             * @description Signed JWT to send as `Authorization: Bearer`.
             * @example eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIwZjJjOGQxNCJ9.placeholder-signature
             */
            accessToken?: string;
            /** @example Bearer */
            tokenType?: string;
            /**
             * Format: int64
             * @description Seconds until the access token expires.
             * @example 900
             */
            expiresInSeconds?: number;
            /**
             * @description Single-use token for renewing the session. Rotated on every renewal. Present for native clients only; a request sent with `X-Rikkaus-Client: web` receives it in the HttpOnly `rikkaus_refresh` cookie and this field is omitted.
             * @example 1mQ7rVx0Yb3TkPzFhLdW8sNcJgA6eRuq2ZoXiB4ySvE
             */
            refreshToken?: string;
            user?: components["schemas"]["AuthenticatedUserResponse"];
        };
        /** @description Completes a Google sign-in started by the client. */
        GoogleSignInRequest: {
            /**
             * @description The authorization code Google returned to the client's redirect URI.
             * @example 4/0AbUR2VPlaceholderAuthorizationCode
             */
            authorizationCode: string;
            /**
             * @description The PKCE code verifier for the challenge sent when the flow started.
             * @example dBjftJeZ4CVPmB92K27uhbUJU1p1r-wW1gFWFOEjXk
             */
            codeVerifier: string;
            /**
             * @description The redirect URI used to obtain the code. Must appear in the server's allowlist.
             * @example https://app.rikkaus.example/auth/callback
             */
            redirectUri: string;
        };
        /** @description Create an asset with the fields every category shares. */
        CreateAssetRequest: {
            /**
             * @description What the user calls this asset.
             * @example Tài khoản USD
             */
            name: string;
            /**
             * @description One of the eleven asset categories.
             * @example BANK_ACCOUNT
             * @enum {string}
             */
            category: "CASH" | "BANK_ACCOUNT" | "TERM_DEPOSIT" | "LISTED_SECURITIES" | "BOND" | "INVESTMENT_FUND" | "REAL_ESTATE" | "PRIVATE_BUSINESS_EQUITY" | "VEHICLE_OR_VALUABLE_ASSET" | "COLLECTIBLE" | "OTHER_ASSET";
            /**
             * @description What the asset is worth, in this asset's currency. Entered by the user; there is no price feed. Zero is allowed, negative is not.
             * @example 20000
             */
            currentValue: number;
            /**
             * @description The currency the values are expressed in. The reporting currency is separately fixed to VND and is read from the settings resource.
             * @example USD
             * @enum {string}
             */
            currency: "VND" | "USD";
            /**
             * Format: date
             * @description The date the user states this value was accurate. Must not be in the future, judged as a Vietnamese civil date.
             * @example 2026-09-12
             */
            valuationDate: string;
            /**
             * @description The USD/VND rate the user applied to currentValue, kept with the first record of the valuation history as the rate used. Only for a USD asset, and only together with rateEffectiveDate.
             * @example 25100
             */
            usdToVndRate?: number;
            /**
             * Format: date
             * @description The date the user states that rate took effect. Must not be in the future. Only together with usdToVndRate.
             * @example 2026-09-12
             */
            rateEffectiveDate?: string;
            /**
             * @description How readily the user judges this asset can be turned into cash.
             * @example HIGH
             * @enum {string}
             */
            liquidity: "HIGH" | "MEDIUM" | "LOW";
            /**
             * @description Purchase value or cost basis, in the same currency.
             * @example 19500
             */
            purchaseValue?: number;
            /**
             * Format: date
             * @description When the asset was acquired. Must not be in the future and must not be later than the valuation date.
             * @example 2024-03-18
             */
            purchaseDate?: string;
            /**
             * @description Units held, where that is meaningful. Must be greater than zero.
             * @example 20000
             */
            quantity?: number;
            /**
             * @description Where the asset is held or stored.
             * @example Ngân hàng An Tín
             */
            institution?: string;
            /**
             * @description Anything the user wants to remember about it.
             * @example Tiền gửi dự phòng bằng USD.
             */
            notes?: string;
            /**
             * @description Ticker, ISIN or fund code, as the user types it. Nothing resolves it against a market. Accepted for LISTED_SECURITIES, BOND, INVESTMENT_FUND and OTHER_ASSET; any other category refuses it.
             * @example VCB
             */
            symbol?: string;
            /**
             * @description What one unit cost, in this asset's own currency. Entered by the user and never multiplied by the quantity. Accepted for the same four categories as the symbol.
             * @example 21800
             */
            costPerUnit?: number;
            /**
             * @description What one unit is worth now, in this asset's own currency. Entered by the user; currentValue is a separate field and is never derived from this one. Accepted for the same four categories as the symbol.
             * @example 24150
             */
            currentPricePerUnit?: number;
            /**
             * @description Interest rate or expected yield as a percent per year, so 6.2500 is 6.25%. No interest is accrued and no yield projected from it. Accepted for BOND, TERM_DEPOSIT and OTHER_ASSET only: a listed security has no rate, and a fund's expected return belongs in the notes.
             * @example 6.25
             */
            interestRate?: number;
        };
        /** @description Record what an asset is worth. Appended to the history; the asset's current value follows when this is the newest record. */
        RecordValuationRequest: {
            /**
             * @description What the asset is worth, in the asset's own currency. The original amount, never a converted one. Zero is allowed, negative is not.
             * @example 21250
             */
            value: number;
            /**
             * @description The currency of the value. Must be the asset's currency.
             * @example USD
             * @enum {string}
             */
            currency: "VND" | "USD";
            /**
             * Format: date
             * @description The date the user states this value was true. Must not be in the future and not before the asset's purchase date, judged as a Vietnamese civil date. Decides whether this record becomes the current one.
             * @example 2026-09-14
             */
            effectiveDate: string;
            /**
             * @description The USD/VND rate the user applied to this value: a snapshot kept with the record, not a reference to the settings rate. Only on a USD record, and only together with rateEffectiveDate.
             * @example 25100
             */
            usdToVndRate?: number;
            /**
             * Format: date
             * @description The date the user states that rate took effect. Must not be in the future. Only together with usdToVndRate.
             * @example 2026-09-12
             */
            rateEffectiveDate?: string;
            /**
             * @description Why this record exists. Required when correcting an earlier record; otherwise optional.
             * @example Cập nhật số dư sau khi đối chiếu sao kê.
             */
            note?: string;
            /**
             * Format: uuid
             * @description The earlier record of this asset that this one corrects. The original is kept unchanged and stays readable; this record carries the reason in note.
             * @example 6d2f8a41-3b9c-4e07-8f15-2a7c4e9b0d63
             */
            correctsValuationId?: string;
        };
        /** @description One record in an asset's valuation history. */
        AssetValuationResponse: {
            /**
             * Format: uuid
             * @description The record's identifier.
             * @example 9c4e2b7a-1d35-4f68-a0c2-7e5b3d9f1a24
             */
            id?: string;
            /**
             * Format: uuid
             * @description The asset this record belongs to.
             * @example 3f1c9a52-8d4b-4f0e-9c21-5b7a8e6d4f10
             */
            assetId?: string;
            /**
             * @description What the asset was worth, in its own currency.
             * @example 21250
             */
            value?: number;
            /**
             * @description The currency of the value.
             * @example USD
             * @enum {string}
             */
            currency?: "VND" | "USD";
            /**
             * Format: date
             * @description The date the user states the value was true.
             * @example 2026-09-14
             */
            effectiveDate?: string;
            /**
             * @description The USD/VND rate the user applied when recording this value. Null on a VND record, and null when the user did not capture it.
             * @example 25100
             */
            usdToVndRate?: number;
            /**
             * Format: date
             * @description The date the user states that rate took effect. Null whenever usdToVndRate is.
             * @example 2026-09-12
             */
            rateEffectiveDate?: string;
            /**
             * @description The user's note, or the reason for a correction. Null when not given.
             * @example Cập nhật số dư sau khi đối chiếu sao kê.
             */
            note?: string;
            /**
             * Format: uuid
             * @description The record this one corrects, when it is a correction. Null otherwise.
             * @example 6d2f8a41-3b9c-4e07-8f15-2a7c4e9b0d63
             */
            correctsValuationId?: string;
            /**
             * @description True on exactly one record of an asset: the one its current value, currency and valuation date are taken from. Latest effective date wins; between two on the same date, the later recorded.
             * @example true
             */
            current?: boolean;
            /**
             * Format: date-time
             * @description When the server stored this record.
             * @example 2026-09-14T03:11:47.902Z
             */
            recordedAt?: string;
        };
        /** @description Record progress through the guided onboarding. Send only what changed. */
        UpdateOnboardingRequest: {
            /**
             * Format: int32
             * @description The step the user is now viewing, 1 to 7 in the backbone order. The client resumes here next time.
             * @example 3
             */
            currentStep?: number;
            /**
             * @description Whether the user has reviewed their first dashboard. true records the first review and keeps its moment if sent again; false withdraws it.
             * @example true
             */
            dashboardReviewed?: boolean;
            /**
             * @description COMPLETED or DISMISSED ends the guided flow; IN_PROGRESS reopens it. Completion does not require every step to be done.
             * @example COMPLETED
             * @enum {string}
             */
            status?: "IN_PROGRESS" | "COMPLETED" | "DISMISSED";
        };
        /** @description The signed-in user's guided-onboarding checklist and where they are in it. */
        OnboardingResponse: {
            /**
             * @description Where the user stands in the flow as a whole.
             * @example IN_PROGRESS
             * @enum {string}
             */
            status?: "IN_PROGRESS" | "COMPLETED" | "DISMISSED";
            /**
             * Format: int32
             * @description The step the user last viewed, 1 to 7. 1 until the client records one.
             * @example 3
             */
            currentStep?: number;
            /** @description All seven steps, always in backbone order. */
            steps?: components["schemas"]["OnboardingStepState"][];
            /**
             * Format: date-time
             * @description When the user first reviewed their dashboard. Null until the client records it.
             * @example 2026-11-14T12:05:41.208Z
             */
            dashboardReviewedAt?: string;
            /**
             * Format: date-time
             * @description When the user completed the flow. Null unless status is COMPLETED.
             */
            completedAt?: string;
            /**
             * Format: date-time
             * @description When the user dismissed the flow. Null unless status is DISMISSED.
             */
            dismissedAt?: string;
        };
        /** @description One checklist step and whether it is done now. */
        OnboardingStepState: {
            /**
             * Format: int32
             * @description The step's position, 1 to 7.
             * @example 1
             */
            step?: number;
            /**
             * @description Which step this is. The client maps it to its copy.
             * @example EXCHANGE_RATE
             * @enum {string}
             */
            key?: "EXCHANGE_RATE" | "CASH_AND_BANK_ACCOUNTS" | "MAJOR_ASSETS" | "LIABILITIES" | "RECURRING_CASH_FLOWS" | "DASHBOARD_REVIEW" | "FIRST_GOAL";
            /**
             * @description Whether the user's records satisfy this step right now. Deleting the last such record makes it false again.
             * @example true
             */
            done?: boolean;
        };
        MetaResponse: {
            application?: string;
            apiVersion?: string;
            buildVersion?: string;
            /** Format: date-time */
            serverTime?: string;
        };
        /** @description A goal template: a type and the suggested defaults for a new goal of that type. */
        GoalTemplateResponse: {
            /**
             * @description The type a goal created from this template carries.
             * @example EMERGENCY_FUND
             * @enum {string}
             */
            type?: "EMERGENCY_FUND" | "HOME_PURCHASE" | "EDUCATION" | "RETIREMENT" | "NET_WORTH_GROWTH" | "DEBT_REDUCTION" | "OTHER_GOAL";
            /**
             * @description The suggested expected annual return, in percent. A suggestion, not a promise.
             * @example 3
             */
            defaultExpectedAnnualReturnPercent?: number;
            /**
             * Format: int32
             * @description How many months from today to suggest as the target date.
             * @example 18
             */
            defaultHorizonMonths?: number;
            /**
             * @description Which amount the form should suggest, and from what figure the client already holds.
             * @example TARGET_FROM_SIX_MONTHS_OF_EXPENSES
             * @enum {string}
             */
            amountPrefill?: "NONE" | "TARGET_FROM_SIX_MONTHS_OF_EXPENSES" | "ALLOCATED_FROM_CURRENT_NET_WORTH" | "TARGET_FROM_TOTAL_LIABILITIES";
        };
        /** @description Income, expense, net, composition and passive income of one calendar month, in VND. */
        MonthlyCashFlowReport: {
            /**
             * @description The month reported, YYYY-MM.
             * @example 2026-09
             */
            month?: string;
            /**
             * Format: date
             * @description The first day of the month, included.
             * @example 2026-09-01
             */
            from?: string;
            /**
             * Format: date
             * @description The last day of the month, included.
             * @example 2026-09-30
             */
            to?: string;
            /**
             * @description The currency every amount except originalAmount and unconverted is in.
             * @example VND
             * @enum {string}
             */
            currency?: "VND" | "USD";
            /** @description The USD/VND rate the user has declared, applied to every USD line. Null when none is declared, in which case no USD line is converted. */
            exchangeRate?: components["schemas"]["MonthlyCashFlowReportRate"];
            /**
             * @description False when a USD line could not be converted for lack of a rate. The totals then cover only the converted lines and unconverted says what was left out.
             * @example true
             */
            complete?: boolean;
            totals?: components["schemas"]["MonthlyCashFlowReportTotals"];
            unconverted?: components["schemas"]["MonthlyCashFlowReportUnconverted"];
            /** @description Income by category, largest amount first, then by category name. */
            incomeComposition?: components["schemas"]["MonthlyCashFlowReportCategoryShare"][];
            /** @description Expense by category, largest amount first, then by category name. */
            expenseComposition?: components["schemas"]["MonthlyCashFlowReportCategoryShare"][];
            passiveIncome?: components["schemas"]["MonthlyCashFlowReportPassiveIncome"];
            /** @description Every occurrence in the month, ordered by date and then by sourceEntryId, as GET /api/v1/cash-flow-occurrences orders them. */
            occurrences?: components["schemas"]["MonthlyCashFlowReportLine"][];
        };
        /** @description One asset's linked income in the month. */
        MonthlyCashFlowReportAssetIncome: {
            /**
             * Format: uuid
             * @description The asset; GET /api/v1/assets/{assetId} reads its name.
             * @example 6a1f3c2e-8b47-4d09-9e5a-1c7b2d4f8e60
             */
            assetId?: string;
            /**
             * @description Sum of the lines linked to this asset.
             * @example 18000000
             */
            amount?: number;
            /**
             * @description amount as a percent of total income, one decimal, rounded on its own.
             * @example 10
             */
            sharePercentOfIncome?: number;
            /**
             * Format: int32
             * @example 1
             */
            occurrenceCount?: number;
            /** @example false */
            partial?: boolean;
        };
        /** @description One category's part of its type's total. */
        MonthlyCashFlowReportCategoryShare: {
            /**
             * @example HOUSING
             * @enum {string}
             */
            category?: "SALARY" | "BUSINESS_INCOME" | "RENTAL_INCOME" | "DIVIDEND" | "DEPOSIT_INTEREST" | "BOND_OR_FUND_INCOME" | "OTHER_INCOME" | "LIVING_COSTS" | "HOUSING" | "EDUCATION" | "HEALTHCARE" | "INSURANCE" | "TAX_AND_FEES" | "DEBT_PRINCIPAL_PAYMENT" | "INTEREST_PAYMENT" | "OTHER_EXPENSE";
            /**
             * @description Sum of the category's converted lines.
             * @example 18750000
             */
            amount?: number;
            /**
             * @description Percent of the type's total at one decimal. The rows of one composition are apportioned by largest remainder so they sum to exactly 100.0. Null when the type's total is zero.
             * @example 20.4
             */
            sharePercent?: number;
            /**
             * Format: int32
             * @description How many lines of the month are in this category.
             * @example 1
             */
            occurrenceCount?: number;
            /**
             * @description True when a line of this category is unconverted and left out of amount.
             * @example false
             */
            partial?: boolean;
        };
        /** @description One occurrence in the month, traceable to its entry by sourceEntryId and occurrenceDate. */
        MonthlyCashFlowReportLine: {
            /**
             * Format: uuid
             * @description The entry; GET /api/v1/cash-flow-entries/{id} reads it.
             * @example 9b4d6f12-7e3a-4c58-b1d9-0a2e5c7f8d61
             */
            sourceEntryId?: string;
            /**
             * Format: date
             * @example 2026-09-15
             */
            occurrenceDate?: string;
            /**
             * Format: int32
             * @description Position in the entry's series, 1 for a one-time entry.
             * @example 2
             */
            sequence?: number;
            /** @example false */
            shiftedToMonthEnd?: boolean;
            /**
             * @example INCOME
             * @enum {string}
             */
            entryType?: "INCOME" | "EXPENSE";
            /**
             * @example DEPOSIT_INTEREST
             * @enum {string}
             */
            category?: "SALARY" | "BUSINESS_INCOME" | "RENTAL_INCOME" | "DIVIDEND" | "DEPOSIT_INTEREST" | "BOND_OR_FUND_INCOME" | "OTHER_INCOME" | "LIVING_COSTS" | "HOUSING" | "EDUCATION" | "HEALTHCARE" | "INSURANCE" | "TAX_AND_FEES" | "DEBT_PRINCIPAL_PAYMENT" | "INTEREST_PAYMENT" | "OTHER_EXPENSE";
            /**
             * @description Null for a one-time entry.
             * @example YEARLY
             * @enum {string}
             */
            recurrence?: "MONTHLY" | "QUARTERLY" | "YEARLY";
            /**
             * Format: uuid
             * @description The asset the entry is linked to. Null when it has none.
             */
            linkedAssetId?: string;
            /**
             * @description The entry's note. Null when it has none.
             * @example Lãi tiền gửi USD
             */
            notes?: string;
            /**
             * @description The entry's amount in its own currency.
             * @example 1200
             */
            originalAmount?: number;
            /**
             * @example USD
             * @enum {string}
             */
            originalCurrency?: "VND" | "USD";
            /**
             * @description The amount in VND: the original for a VND entry, originalAmount times exchangeRate.usdToVndRate rounded half-up to whole dong for a USD one. Null for a USD entry when no rate is declared.
             * @example 30120000
             */
            amount?: number;
        };
        /** @description Income linked to an asset, grouped by that asset. Passive income is decided by the link the user entered, never inferred from a category. */
        MonthlyCashFlowReportPassiveIncome: {
            /**
             * @description Sum of byAsset.
             * @example 22500000
             */
            total?: number;
            /**
             * @description total as a percent of total income, one decimal. Null when there is no income.
             * @example 12.5
             */
            sharePercentOfIncome?: number;
            /** @description One row per linked asset, largest amount first, then by assetId. */
            byAsset?: components["schemas"]["MonthlyCashFlowReportAssetIncome"][];
            unassigned?: components["schemas"]["MonthlyCashFlowReportUnassigned"];
        };
        /** @description The declared rate the report applied. */
        MonthlyCashFlowReportRate: {
            /**
             * @description How many VND one USD buys.
             * @example 25100
             */
            usdToVndRate?: number;
            /**
             * Format: date
             * @description The date the user declared the rate took effect.
             * @example 2026-09-12
             */
            effectiveDate?: string;
        };
        /** @description The month's totals over converted lines. */
        MonthlyCashFlowReportTotals: {
            /**
             * @description Sum of the INCOME lines' amount.
             * @example 180000000
             */
            income?: number;
            /**
             * @description Sum of the EXPENSE lines' amount.
             * @example 92000000
             */
            expense?: number;
            /**
             * @description income minus expense, and nothing else. Negative when spending exceeds income.
             * @example 88000000
             */
            net?: number;
        };
        /** @description Income in RENTAL_INCOME, DIVIDEND, DEPOSIT_INTEREST or BOND_OR_FUND_INCOME with no asset link. Shown so it can be linked; not counted in passive income. */
        MonthlyCashFlowReportUnassigned: {
            /** @example 30120000 */
            amount?: number;
            /**
             * Format: int32
             * @example 1
             */
            occurrenceCount?: number;
            /** @example false */
            partial?: boolean;
        };
        /** @description The USD lines left out of every total because no rate is declared. */
        MonthlyCashFlowReportUnconverted: {
            /**
             * Format: int32
             * @description How many lines have a null amount.
             * @example 0
             */
            occurrenceCount?: number;
            /**
             * @description Sum of the unconverted INCOME lines' originalAmount, in USD.
             * @example 0
             */
            income?: number;
            /**
             * @description Sum of the unconverted EXPENSE lines' originalAmount, in USD.
             * @example 0
             */
            expense?: number;
        };
        /** @description A date on which one of the signed-in user's cash-flow entries applies. */
        CashFlowOccurrenceResponse: {
            /**
             * Format: uuid
             * @description The entry this occurrence comes from; GET /api/v1/cash-flow-entries/{id} reads it.
             * @example 9b4d6f12-7e3a-4c58-b1d9-0a2e5c7f8d61
             */
            sourceEntryId?: string;
            /**
             * Format: date
             * @description The calendar date the occurrence falls on.
             * @example 2026-02-28
             */
            occurrenceDate?: string;
            /**
             * Format: int32
             * @description The occurrence's position in its entry's series, counted from the effective date. Always 1 for a one-time entry.
             * @example 2
             */
            sequence?: number;
            /**
             * @description True when the entry's day of month does not exist in this month, so the occurrence was moved back to the month's last day.
             * @example true
             */
            shiftedToMonthEnd?: boolean;
            /**
             * @description INCOME or EXPENSE, as on the entry.
             * @example INCOME
             * @enum {string}
             */
            entryType?: "INCOME" | "EXPENSE";
            /**
             * @description The entry's category.
             * @example SALARY
             * @enum {string}
             */
            category?: "SALARY" | "BUSINESS_INCOME" | "RENTAL_INCOME" | "DIVIDEND" | "DEPOSIT_INTEREST" | "BOND_OR_FUND_INCOME" | "OTHER_INCOME" | "LIVING_COSTS" | "HOUSING" | "EDUCATION" | "HEALTHCARE" | "INSURANCE" | "TAX_AND_FEES" | "DEBT_PRINCIPAL_PAYMENT" | "INTEREST_PAYMENT" | "OTHER_EXPENSE";
            /**
             * @description The entry's amount, in its own currency. Always positive.
             * @example 45000000
             */
            amount?: number;
            /**
             * @description The currency the amount is expressed in.
             * @example VND
             * @enum {string}
             */
            currency?: "VND" | "USD";
            /**
             * @description MONTHLY, QUARTERLY or YEARLY for a recurring entry. Null for a one-time one.
             * @example MONTHLY
             * @enum {string}
             */
            recurrence?: "MONTHLY" | "QUARTERLY" | "YEARLY";
        };
        /** @description The three whole calendar months the forecast covers. */
        CashFlowForecastHorizon: {
            /**
             * Format: date
             * @description The first day, included.
             * @example 2026-12-01
             */
            from?: string;
            /**
             * Format: date
             * @description The last day, included.
             * @example 2027-02-28
             */
            to?: string;
            months?: string[];
        };
        /** @description One projected amount and the record it comes from. */
        CashFlowForecastItem: {
            /**
             * Format: date
             * @description The day the amount lands.
             * @example 2027-01-15
             */
            date?: string;
            /**
             * @description INCOME raises the projected cash, EXPENSE lowers it.
             * @example EXPENSE
             * @enum {string}
             */
            direction?: "INCOME" | "EXPENSE";
            /**
             * @description CASH_FLOW_ENTRY or LIABILITY.
             * @example LIABILITY
             * @enum {string}
             */
            sourceType?: "CASH_FLOW_ENTRY" | "LIABILITY" | "ASSET";
            /**
             * Format: uuid
             * @description The record; GET /api/v1/cash-flow-entries/{id} or /api/v1/liabilities/{id} reads it.
             * @example 8e2f4a90-1c37-4d5b-b6a8-0f9d3e7c2b14
             */
            sourceId?: string;
            /**
             * @description The record's category.
             * @example BANK_LOAN
             */
            category?: string;
            /**
             * @description The amount in the record's own currency.
             * @example 18500000
             */
            amount?: number;
            /**
             * @description The record's currency.
             * @example VND
             * @enum {string}
             */
            currency?: "VND" | "USD";
            /**
             * @description The amount in VND, rounded to whole đồng.
             * @example 18500000
             */
            amountVnd?: number;
            /**
             * @description MONTHLY, QUARTERLY or YEARLY for an entry; MONTHLY, QUARTERLY, SEMIANNUAL or YEARLY for a liability; null for a one-time entry.
             * @example MONTHLY
             */
            cadence?: string | null;
            /**
             * Format: int32
             * @description The date's position in the record's series: counted from an entry's effective date, or from a liability's next payment date.
             * @example 2
             */
            sequence?: number;
            /**
             * @description True when the record's day of month does not exist in this month.
             * @example false
             */
            shiftedToMonthEnd?: boolean;
        };
        /** @description One calendar month of the horizon. */
        CashFlowForecastMonth: {
            /** @example 2027-01 */
            month?: string;
            /**
             * @description The VND sum of the month's INCOME items.
             * @example 105000000
             */
            income?: number;
            /**
             * @description The VND sum of the month's EXPENSE items.
             * @example 90000000
             */
            expense?: number;
            /**
             * @description income minus expense.
             * @example 15000000
             */
            net?: number;
            /**
             * @description Projected cash at the start of the month's first day.
             * @example 124265000
             */
            openingBalance?: number;
            /**
             * @description Projected cash at the end of the month's last day.
             * @example 139265000
             */
            closingBalance?: number;
            /**
             * @description The lowest of the month's opening balance and every end-of-day balance on a day an amount lands.
             * @example 89065000
             */
            lowestBalance?: number;
            /**
             * Format: date
             * @description The earliest date the lowest balance is reached.
             * @example 2027-01-20
             */
            lowestBalanceDate?: string;
            /**
             * @description True when an excluded record would have contributed to this month: a liability falling due in it without a schedule, or a USD amount without a rate.
             * @example true
             */
            partial?: boolean;
            /** @description Ordered by date, then CASH_FLOW_ENTRY before LIABILITY, then sourceId. */
            items?: components["schemas"]["CashFlowForecastItem"][];
        };
        /** @description The cash the projection starts from, applied at the start of the horizon's first day. */
        CashFlowForecastOpeningBalance: {
            /**
             * @description The sum of the counted sources, in VND.
             * @example 147200000
             */
            amount?: number;
            /**
             * Format: date
             * @description The first day of the horizon.
             * @example 2026-12-01
             */
            appliedOn?: string;
            /** @description The cash and bank accounts counted, ordered by sourceId. A USD account without a declared rate is in excludedSources instead. */
            sources?: components["schemas"]["CashFlowForecastOpeningSource"][];
        };
        /** @description A cash or bank account counted in the opening balance. */
        CashFlowForecastOpeningSource: {
            /**
             * @description Always ASSET.
             * @example ASSET
             * @enum {string}
             */
            sourceType?: "CASH_FLOW_ENTRY" | "LIABILITY" | "ASSET";
            /**
             * Format: uuid
             * @description The asset; GET /api/v1/assets/{id} reads it.
             * @example 3d7a51c2-8f04-4b6e-9a21-c5e0f8b2d419
             */
            sourceId?: string;
            /**
             * @description CASH or BANK_ACCOUNT.
             * @example BANK_ACCOUNT
             */
            category?: string;
            /**
             * Format: date
             * @description The date the user states the value was true.
             * @example 2026-11-20
             */
            valuationDate?: string;
            /**
             * @description The value in the asset's own currency.
             * @example 2000
             */
            amount?: number;
            /**
             * @description The asset's currency.
             * @example USD
             * @enum {string}
             */
            currency?: "VND" | "USD";
            /**
             * @description The value in VND.
             * @example 50200000
             */
            amountVnd?: number;
        };
        /** @description A three-month projection of the signed-in user's cash from the records they can see. */
        CashFlowForecastResponse: {
            horizon?: components["schemas"]["CashFlowForecastHorizon"];
            openingBalance?: components["schemas"]["CashFlowForecastOpeningBalance"];
            months?: components["schemas"]["CashFlowForecastMonth"][];
            minimumBalance?: components["schemas"]["CashFlowForecastMinimumBalance"];
            /** @description Records that could have contributed and do not, each with its reason. Ordered by sourceType, then sourceId. */
            excludedSources?: components["schemas"]["CashFlowForecastExclusion"][];
            /**
             * @description True when any month is partial or a cash or bank account could not be counted in the opening balance.
             * @example true
             */
            partial?: boolean;
            assumptions?: components["schemas"]["CashFlowForecastAssumptions"];
        };
        /** @description The inputs the projection rests on. */
        CashFlowForecastAssumptions: {
            /**
             * Format: date
             * @description The day the forecast was computed, in timeZone.
             * @example 2026-11-22
             */
            asOfDate?: string;
            /**
             * @description The zone today is read in.
             * @example Asia/Ho_Chi_Minh
             */
            timeZone?: string;
            /**
             * @description The currency every total is expressed in.
             * @example VND
             * @enum {string}
             */
            currency?: "VND" | "USD";
            /** @description The rate USD amounts were converted with. Null when the user has declared none. */
            exchangeRate?: components["schemas"]["CashFlowForecastExchangeRate"];
            /** @description The asset categories the opening balance is made of. */
            openingBalanceCategories?: string[];
            /** @description The days between today and the horizon that are not projected. Null when there are none. */
            notProjected?: components["schemas"]["CashFlowForecastDateRange"];
        };
        /** @description A range of calendar days, both included. */
        CashFlowForecastDateRange: {
            /**
             * Format: date
             * @example 2026-11-23
             */
            from?: string;
            /**
             * Format: date
             * @example 2026-11-30
             */
            to?: string;
        };
        /** @description The user-declared USD/VND rate the forecast used. */
        CashFlowForecastExchangeRate: {
            /**
             * @description How many VND one USD buys.
             * @example 25100
             */
            usdToVndRate?: number;
            /**
             * Format: date
             * @description The date the user declares the rate took effect.
             * @example 2026-09-12
             */
            effectiveDate?: string;
        };
        /** @description A record that could have contributed to the forecast and does not. */
        CashFlowForecastExclusion: {
            /**
             * @example LIABILITY
             * @enum {string}
             */
            sourceType?: "CASH_FLOW_ENTRY" | "LIABILITY" | "ASSET";
            /**
             * Format: uuid
             * @example c41b7e2d-6a95-4f08-8d3c-27e1b9f05a6e
             */
            sourceId?: string;
            /**
             * @example NO_PAYMENT_SCHEDULE
             * @enum {string}
             */
            reason?: "NO_OCCURRENCE_IN_HORIZON" | "NO_PAYMENT_SCHEDULE" | "NO_NEXT_PAYMENT_DATE" | "NO_PAYMENT_IN_HORIZON" | "COVERED_BY_LINKED_ENTRIES" | "EXCHANGE_RATE_MISSING";
            /** @description The horizon months it would have contributed to, when known. Each is marked partial. */
            affectedMonths?: string[];
            /** @description For COVERED_BY_LINKED_ENTRIES, the recurring entries linked to the liability that count it instead. Empty otherwise. */
            linkedEntryIds?: string[];
        };
        /** @description The lowest projected cash position in the horizon and how it reads. */
        CashFlowForecastMinimumBalance: {
            /**
             * Format: date
             * @description The earliest date the lowest position is reached.
             * @example 2027-01-20
             */
            date?: string;
            /** @example 2027-01 */
            month?: string;
            /**
             * @description The lowest position, in VND.
             * @example 89065000
             */
            amount?: number;
            /**
             * @description True when the lowest position is below zero.
             * @example false
             */
            negative?: boolean;
            /**
             * @description One month of projected expense: the horizon's expense divided by three.
             * @example 78166666.67
             */
            referenceThreshold?: number;
            /**
             * @description INSUFFICIENT_DATA when no cash or bank account was counted; else NEGATIVE below zero, THIN below the threshold, ADEQUATE otherwise.
             * @example ADEQUATE
             * @enum {string}
             */
            status?: "ADEQUATE" | "THIN" | "NEGATIVE" | "INSUFFICIENT_DATA";
        };
    };
    responses: never;
    parameters: never;
    requestBodies: never;
    headers: {
        /** @description Correlation identifier. Send one matching ^[A-Za-z0-9-]{8,64}$ to have it echoed; anything else is replaced with a generated value and the request still succeeds. Always returned. */
        "X-Correlation-Id": string;
    };
    pathItems: never;
}
export type $defs = Record<string, never>;
export interface operations {
    read: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description The settings of the signed-in user */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ExchangeRateResponse"];
                };
            };
            /** @description No access token, or it has expired or been revoked */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Wrong HTTP method */
            405: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested media type cannot be produced */
            406: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    declare: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["DeclareExchangeRateRequest"];
            };
        };
        responses: {
            /** @description The rate was stored */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ExchangeRateResponse"];
                };
            };
            /** @description The rate is missing or not positive, or the effective date is missing or in the future */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description No access token, or it has expired or been revoked */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Wrong HTTP method */
            405: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The request body is not JSON */
            415: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    read_1: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description The profile of the signed-in user */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ProfileResponse"];
                };
            };
            /** @description No access token, or it has expired or been revoked */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Wrong HTTP method */
            405: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested media type cannot be produced */
            406: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    update: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["UpdateProfileRequest"];
            };
        };
        responses: {
            /** @description The profile after the change */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ProfileResponse"];
                };
            };
            /** @description The display name is blank or longer than 120 characters */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description No access token, or it has expired or been revoked */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Wrong HTTP method */
            405: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The request body is not JSON */
            415: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    read_2: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                liabilityId: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description The liability */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["LiabilityResponse"];
                };
            };
            /** @description The identifier in the path is not a UUID */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description No access token, or it has expired or been revoked */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description No such liability, or it belongs to another user */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Wrong HTTP method */
            405: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested media type cannot be produced */
            406: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    replace: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                liabilityId: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["UpdateLiabilityRequest"];
            };
        };
        responses: {
            /** @description The liability as it now stands */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["LiabilityResponse"];
                };
            };
            /** @description A required field is missing, an amount is negative or too precise, a recurring payment is not positive or is half-stated, a date is inconsistent, or the collateral is not an asset the signed-in user owns */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description No access token, or it has expired or been revoked */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description No such liability, it belongs to another user, or a concurrent request removed it first */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Wrong HTTP method */
            405: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested media type cannot be produced */
            406: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The request body is not JSON */
            415: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    delete: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                liabilityId: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description The liability no longer exists */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
            /** @description The identifier in the path is not a UUID */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description No access token, or it has expired or been revoked */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description No such liability, it belongs to another user, or a concurrent request removed it first */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Wrong HTTP method */
            405: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    read_3: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                goalId: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description The goal */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["GoalResponse"];
                };
            };
            /** @description The identifier in the path is not a UUID */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description No access token, or it has expired or been revoked */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description No such goal, or it belongs to another user */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Wrong HTTP method */
            405: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested media type cannot be produced */
            406: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    replace_1: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                goalId: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["GoalRequest"];
            };
        };
        responses: {
            /** @description The goal as it now stands */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["GoalResponse"];
                };
            };
            /** @description A required field is missing, an amount is negative, zero where it must be positive or too precise, the allocated amount exceeds the target, the return is outside 0 to 30, or a changed target date is not after today */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description No access token, or it has expired or been revoked */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description No such goal, it belongs to another user, or a concurrent request removed it first */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Wrong HTTP method */
            405: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested media type cannot be produced */
            406: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The request body is not JSON */
            415: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    delete_1: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                goalId: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description The goal no longer exists */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
            /** @description The identifier in the path is not a UUID */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description No access token, or it has expired or been revoked */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description No such goal, it belongs to another user, or a concurrent request removed it first */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Wrong HTTP method */
            405: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    read_4: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                entryId: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description The entry */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["CashFlowEntryResponse"];
                };
            };
            /** @description The identifier in the path is not a UUID */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description No access token, or it has expired or been revoked */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description No such entry, or it belongs to another user */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Wrong HTTP method */
            405: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested media type cannot be produced */
            406: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    replace_2: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                entryId: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CashFlowEntryRequest"];
            };
        };
        responses: {
            /** @description The entry as it now stands */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["CashFlowEntryResponse"];
                };
            };
            /** @description The same refusals as on create, or the identifier in the path is not a UUID */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description No access token, or it has expired or been revoked */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description No such entry, it belongs to another user, or a concurrent request removed it first */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Wrong HTTP method */
            405: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested media type cannot be produced */
            406: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The request body is not JSON */
            415: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    delete_2: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                entryId: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description The entry no longer exists */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
            /** @description The identifier in the path is not a UUID */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description No access token, or it has expired or been revoked */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description No such entry, it belongs to another user, or a concurrent request removed it first */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Wrong HTTP method */
            405: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    read_5: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                assetId: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description The asset */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["AssetResponse"];
                };
            };
            /** @description The identifier in the path is not a UUID */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description No access token, or it has expired or been revoked */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description No such asset, or it belongs to another user */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Wrong HTTP method */
            405: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested media type cannot be produced */
            406: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    replaceMetadata: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                assetId: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["UpdateAssetMetadataRequest"];
            };
        };
        responses: {
            /** @description The asset as it now stands */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["AssetResponse"];
                };
            };
            /** @description The name or liquidity is missing, a value is negative or too precise, a quantity is not positive, the purchase date is in the future or later than the asset's stored valuation date, or an instrument field was sent for an asset whose stored category does not accept it */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description No access token, or it has expired or been revoked */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description No such asset, it belongs to another user, or a concurrent request removed it first */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Wrong HTTP method */
            405: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested media type cannot be produced */
            406: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The request body is not JSON */
            415: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    delete_3: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                assetId: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description The asset no longer exists */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
            /** @description The identifier in the path is not a UUID */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description No access token, or it has expired or been revoked */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description No such asset, it belongs to another user, or a concurrent request removed it first */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Wrong HTTP method */
            405: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    list: {
        parameters: {
            query?: {
                /**
                 * @description Searches the name and notes. Substring match that ignores letter case and Vietnamese diacritics, so "thao dien" finds "Căn hộ Thảo Điền". Surrounding spaces are ignored and an empty value means no search. % and _ are literal characters, not wildcards. At most 120 characters; a longer value or one containing U+0000 is rejected as validation-failed naming q.
                 * @example vietcombank
                 */
                q?: string;
                /**
                 * @description Keeps the liabilities in these categories. Repeat the parameter or separate values with commas; a record matching any of them is kept, and different parameters must all match. Values are the exact constant names. An empty value is ignored; any other unknown value is rejected as validation-failed naming this parameter.
                 * @example BANK_LOAN
                 */
                category?: ("BANK_LOAN" | "PERSONAL_LOAN" | "MARGIN_LOAN" | "CREDIT_CARD_BALANCE" | "GUARANTEE_OBLIGATION" | "OTHER_LIABILITY")[];
                /**
                 * @description Keeps the liabilities in these currencies. Repeat the parameter or separate values with commas; a record matching any of them is kept, and different parameters must all match. Values are the exact constant names. An empty value is ignored; any other unknown value is rejected as validation-failed naming this parameter.
                 * @example VND
                 */
                currency?: ("VND" | "USD")[];
                /**
                 * @description nextPaymentDate (the default) lists the soonest payment first; -nextPaymentDate the latest first; name and -name by name. A liability with no next payment date is last in both date orders. One key; a leading - sorts descending. Ties are broken by the default order and then by id, so the order is stable between requests. Any other key, or more than one, is rejected as validation-failed naming sort.
                 * @example name
                 */
                sort?: "nextPaymentDate" | "-nextPaymentDate" | "name" | "-name";
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description The viewer's catalog, possibly empty */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["LiabilityResponse"][];
                };
            };
            /** @description A search longer than 120 characters or containing U+0000, an unknown category or currency, or an unknown sort key. Every rejected parameter is named in errors, and the value sent is never repeated. */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description No access token, or it has expired or been revoked */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Wrong HTTP method */
            405: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested media type cannot be produced */
            406: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    create: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CreateLiabilityRequest"];
            };
        };
        responses: {
            /** @description The liability was created */
            201: {
                headers: {
                    /** @description The new liability's URI, as /api/v1/liabilities/{liabilityId}. */
                    Location?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["LiabilityResponse"];
                };
            };
            /** @description A required field is missing, an amount is negative or too precise, a recurring payment is not positive or is half-stated, a date is inconsistent, or the collateral is not an asset the signed-in user owns. An unrecognised category, currency or frequency is a malformed-request instead, because the body cannot be parsed. */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description No access token, or it has expired or been revoked */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Wrong HTTP method */
            405: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested media type cannot be produced */
            406: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The request body is not JSON */
            415: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    list_1: {
        parameters: {
            query?: {
                /**
                 * @description Searches the name. Substring match that ignores letter case and Vietnamese diacritics, so "thao dien" finds "Căn hộ Thảo Điền". Surrounding spaces are ignored and an empty value means no search. % and _ are literal characters, not wildcards. At most 120 characters; a longer value or one containing U+0000 is rejected as validation-failed naming q.
                 * @example quy du phong
                 */
                q?: string;
                /**
                 * @description Keeps the goals created from these templates. Repeat the parameter or separate values with commas; a record matching any of them is kept, and different parameters must all match. Values are the exact constant names. An empty value is ignored; any other unknown value is rejected as validation-failed naming this parameter.
                 * @example EMERGENCY_FUND
                 */
                type?: ("EMERGENCY_FUND" | "HOME_PURCHASE" | "EDUCATION" | "RETIREMENT" | "NET_WORTH_GROWTH" | "DEBT_REDUCTION" | "OTHER_GOAL")[];
                /**
                 * @description targetDate (the default) lists the nearest target date first and then by name; -targetDate the furthest first; name and -name by name. One key; a leading - sorts descending. Ties are broken by the default order and then by id, so the order is stable between requests. Any other key, or more than one, is rejected as validation-failed naming sort.
                 * @example name
                 */
                sort?: "targetDate" | "-targetDate" | "name" | "-name";
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description The viewer's goals, possibly empty */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["GoalResponse"][];
                };
            };
            /** @description A search longer than 120 characters or containing U+0000, an unknown type, or an unknown sort key. Every rejected parameter is named in errors, and the value sent is never repeated. */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description No access token, or it has expired or been revoked */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Wrong HTTP method */
            405: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested media type cannot be produced */
            406: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    create_1: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["GoalRequest"];
            };
        };
        responses: {
            /** @description The goal was created */
            201: {
                headers: {
                    /** @description The new goal's URI, as /api/v1/goals/{goalId}. */
                    Location?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["GoalResponse"];
                };
            };
            /** @description A required field is missing, an amount is negative, zero where it must be positive or too precise, the allocated amount exceeds the target, the return is outside 0 to 30, or the target date is not after today. An unrecognised type is a malformed-request instead, because the body cannot be parsed. */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description No access token, or it has expired or been revoked */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Wrong HTTP method */
            405: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested media type cannot be produced */
            406: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The request body is not JSON */
            415: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    listFeedback: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description The viewer's feedback, possibly empty */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["FeedbackResponse"][];
                };
            };
            /** @description No access token, or it has expired or been revoked */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Wrong HTTP method */
            405: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested media type cannot be produced */
            406: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    sendFeedback: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["FeedbackRequest"];
            };
        };
        responses: {
            /** @description The feedback was stored. No Location header: there is no route for one entry. */
            201: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["FeedbackResponse"];
                };
            };
            /** @description validation-failed when the screen is missing or not a screen code, or the message is missing, blank, longer than 2000 characters or contains U+0000; every problem is reported at once in the errors array. malformed-request when the body cannot be parsed, including an unknown category. */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description No access token, or it has expired or been revoked */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Wrong HTTP method */
            405: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested media type cannot be produced */
            406: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The request body is not JSON */
            415: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    list_2: {
        parameters: {
            query?: {
                /**
                 * @description Searches the notes, the entry's one free-text field. Substring match that ignores letter case and Vietnamese diacritics, so "thao dien" finds "Căn hộ Thảo Điền". Surrounding spaces are ignored and an empty value means no search. % and _ are literal characters, not wildcards. At most 120 characters; a longer value or one containing U+0000 is rejected as validation-failed naming q.
                 * @example hoc phi
                 */
                q?: string;
                /**
                 * @description Keeps the entries of these types. Repeat the parameter or separate values with commas; a record matching any of them is kept, and different parameters must all match. Values are the exact constant names. An empty value is ignored; any other unknown value is rejected as validation-failed naming this parameter.
                 * @example EXPENSE
                 */
                entryType?: ("INCOME" | "EXPENSE")[];
                /**
                 * @description Keeps the entries in these categories. A category that does not belong to a type kept by entryType simply matches nothing. Repeat the parameter or separate values with commas; a record matching any of them is kept, and different parameters must all match. Values are the exact constant names. An empty value is ignored; any other unknown value is rejected as validation-failed naming this parameter.
                 * @example EDUCATION
                 */
                category?: ("SALARY" | "BUSINESS_INCOME" | "RENTAL_INCOME" | "DIVIDEND" | "DEPOSIT_INTEREST" | "BOND_OR_FUND_INCOME" | "OTHER_INCOME" | "LIVING_COSTS" | "HOUSING" | "EDUCATION" | "HEALTHCARE" | "INSURANCE" | "TAX_AND_FEES" | "DEBT_PRINCIPAL_PAYMENT" | "INTEREST_PAYMENT" | "OTHER_EXPENSE")[];
                /**
                 * @description Keeps one-time or recurring entries. Repeat the parameter or separate values with commas; a record matching any of them is kept, and different parameters must all match. Values are the exact constant names. An empty value is ignored; any other unknown value is rejected as validation-failed naming this parameter.
                 * @example RECURRING
                 */
                timing?: ("ONE_TIME" | "RECURRING")[];
                /**
                 * @description Keeps the recurring entries with these recurrences; a one-time entry has none and is never kept by this filter. Repeat the parameter or separate values with commas; a record matching any of them is kept, and different parameters must all match. Values are the exact constant names. An empty value is ignored; any other unknown value is rejected as validation-failed naming this parameter.
                 * @example MONTHLY
                 */
                recurrence?: ("MONTHLY" | "QUARTERLY" | "YEARLY")[];
                /**
                 * @description Keeps the entries in these currencies. Repeat the parameter or separate values with commas; a record matching any of them is kept, and different parameters must all match. Values are the exact constant names. An empty value is ignored; any other unknown value is rejected as validation-failed naming this parameter.
                 * @example VND
                 */
                currency?: ("VND" | "USD")[];
                /**
                 * @description Keeps the entries whose stored effective date is on or after this date, as YYYY-MM-DD. It filters the start date of a recurring entry, not the dates it falls on; those are GET /api/v1/cash-flow-occurrences.
                 * @example 2026-09-01
                 */
                effectiveFrom?: string;
                /**
                 * @description Keeps the entries whose stored effective date is on or before this date, as YYYY-MM-DD. Must not be before effectiveFrom.
                 * @example 2026-09-30
                 */
                effectiveTo?: string;
                /**
                 * @description -effectiveDate (the default) lists the newest effective date first and then the newest created; effectiveDate the oldest first. One key; a leading - sorts descending. Ties are broken by the default order and then by id, so the order is stable between requests. Any other key, or more than one, is rejected as validation-failed naming sort.
                 * @example effectiveDate
                 */
                sort?: "-effectiveDate" | "effectiveDate";
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description The viewer's catalog, possibly empty */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["CashFlowEntryResponse"][];
                };
            };
            /** @description A search longer than 120 characters or containing U+0000, an unknown entry type, category, timing, recurrence or currency, a date that is not a calendar date in the form YYYY-MM-DD, effectiveTo before effectiveFrom, or an unknown sort key. Every rejected parameter is named in errors, and the value sent is never repeated. */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description No access token, or it has expired or been revoked */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Wrong HTTP method */
            405: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested media type cannot be produced */
            406: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    create_2: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CashFlowEntryRequest"];
            };
        };
        responses: {
            /** @description The entry was created */
            201: {
                headers: {
                    /** @description The new entry's URI, as /api/v1/cash-flow-entries/{entryId}. */
                    Location?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["CashFlowEntryResponse"];
                };
            };
            /** @description A required field is missing, the amount is not positive or too precise, the category does not belong to the entry type, the recurrence or end date does not fit the timing, the end date precedes the effective date, a linked asset or liability is not the viewer's, or both links are sent together. Every problem is reported at once in the errors array. An unrecognised constant is a malformed-request instead, because the body cannot be parsed. */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description No access token, or it has expired or been revoked */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Wrong HTTP method */
            405: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested media type cannot be produced */
            406: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The request body is not JSON */
            415: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    renew: {
        parameters: {
            query?: never;
            header?: {
                /**
                 * @description Send `web` from the browser client. The refresh token is then set as the HttpOnly, SameSite=Strict cookie `rikkaus_refresh` (Path=/api/v1/auth) and omitted from the body. Omit the header from a native client to keep the token in the body.
                 * @example web
                 */
                "X-Rikkaus-Client"?: string;
            };
            path?: never;
            cookie?: {
                /** @description The web client's refresh token, set by a previous sign-in or renewal. */
                rikkaus_refresh?: string;
            };
        };
        requestBody?: {
            content: {
                "application/json": components["schemas"]["RefreshSessionRequest"];
            };
        };
        responses: {
            /** @description The session was renewed */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SessionResponse"];
                };
            };
            /** @description No refresh token in the body or in an honoured cookie */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The refresh token is unknown, expired, already used, or revoked */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The refresh cookie was sent without `X-Rikkaus-Client: web`, or from an origin that is not allowlisted. Refused before the token is read, so the session is untouched. */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Wrong HTTP method */
            405: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The request body is not JSON */
            415: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    signOut: {
        parameters: {
            query?: never;
            header?: {
                /**
                 * @description Send `web` from the browser client. The refresh token is then set as the HttpOnly, SameSite=Strict cookie `rikkaus_refresh` (Path=/api/v1/auth) and omitted from the body. Omit the header from a native client to keep the token in the body.
                 * @example web
                 */
                "X-Rikkaus-Client"?: string;
            };
            path?: never;
            cookie?: {
                /** @description The web client's refresh token, set by a previous sign-in or renewal. */
                rikkaus_refresh?: string;
            };
        };
        requestBody?: {
            content: {
                "application/json": components["schemas"]["RefreshSessionRequest"];
            };
        };
        responses: {
            /** @description The session is no longer usable and the cookie is cleared */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
            /** @description No refresh token in the body or in an honoured cookie */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The refresh cookie was sent without `X-Rikkaus-Client: web`, or from an origin that is not allowlisted */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Wrong HTTP method */
            405: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The request body is not JSON */
            415: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    signInWithGoogle: {
        parameters: {
            query?: never;
            header?: {
                /**
                 * @description Send `web` from the browser client. The refresh token is then set as the HttpOnly, SameSite=Strict cookie `rikkaus_refresh` (Path=/api/v1/auth) and omitted from the body. Omit the header from a native client to keep the token in the body.
                 * @example web
                 */
                "X-Rikkaus-Client"?: string;
            };
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["GoogleSignInRequest"];
            };
        };
        responses: {
            /** @description The session was established */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["SessionResponse"];
                };
            };
            /** @description A field is missing, or the redirect URI is not allowlisted */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Google rejected the code, or the identity could not be verified */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description `X-Rikkaus-Client: web` was sent from an origin that is not allowlisted */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Wrong HTTP method */
            405: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The request body is not JSON */
            415: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    list_3: {
        parameters: {
            query?: {
                /**
                 * @description Searches the name, institution and notes. Substring match that ignores letter case and Vietnamese diacritics, so "thao dien" finds "Căn hộ Thảo Điền". Surrounding spaces are ignored and an empty value means no search. % and _ are literal characters, not wildcards. At most 120 characters; a longer value or one containing U+0000 is rejected as validation-failed naming q.
                 * @example thao dien
                 */
                q?: string;
                /**
                 * @description Keeps the assets in these categories. Repeat the parameter or separate values with commas; a record matching any of them is kept, and different parameters must all match. Values are the exact constant names. An empty value is ignored; any other unknown value is rejected as validation-failed naming this parameter.
                 * @example BANK_ACCOUNT
                 */
                category?: ("CASH" | "BANK_ACCOUNT" | "TERM_DEPOSIT" | "LISTED_SECURITIES" | "BOND" | "INVESTMENT_FUND" | "REAL_ESTATE" | "PRIVATE_BUSINESS_EQUITY" | "VEHICLE_OR_VALUABLE_ASSET" | "COLLECTIBLE" | "OTHER_ASSET")[];
                /**
                 * @description Keeps the assets valued in these currencies. Repeat the parameter or separate values with commas; a record matching any of them is kept, and different parameters must all match. Values are the exact constant names. An empty value is ignored; any other unknown value is rejected as validation-failed naming this parameter.
                 * @example VND
                 */
                currency?: ("VND" | "USD")[];
                /**
                 * @description Keeps the assets with these liquidity levels. Repeat the parameter or separate values with commas; a record matching any of them is kept, and different parameters must all match. Values are the exact constant names. An empty value is ignored; any other unknown value is rejected as validation-failed naming this parameter.
                 * @example HIGH
                 */
                liquidity?: ("HIGH" | "MEDIUM" | "LOW")[];
                /**
                 * @description -valuationDate (the default) lists the newest valuation first and then by name; valuationDate the oldest first; name and -name by name. One key; a leading - sorts descending. Ties are broken by the default order and then by id, so the order is stable between requests. Any other key, or more than one, is rejected as validation-failed naming sort.
                 * @example name
                 */
                sort?: "-valuationDate" | "valuationDate" | "name" | "-name";
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description The viewer's catalog, possibly empty */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["AssetResponse"][];
                };
            };
            /** @description A search longer than 120 characters or containing U+0000, an unknown category, currency or liquidity, or an unknown sort key. Every rejected parameter is named in errors, and the value sent is never repeated. */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description No access token, or it has expired or been revoked */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Wrong HTTP method */
            405: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested media type cannot be produced */
            406: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    create_3: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CreateAssetRequest"];
            };
        };
        responses: {
            /** @description The asset was created */
            201: {
                headers: {
                    /** @description The new asset's URI, as /api/v1/assets/{assetId}. */
                    Location?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["AssetResponse"];
                };
            };
            /** @description A required field is missing, a value is negative or too precise, a quantity is not positive, a date is impossible, the rate snapshot is incomplete or given for a VND asset, or an instrument field was sent for a category that does not accept it. An unrecognised category, currency or liquidity is a malformed-request instead, because the body cannot be parsed. */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description No access token, or it has expired or been revoked */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Wrong HTTP method */
            405: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested media type cannot be produced */
            406: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The request body is not JSON */
            415: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    list_4: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                assetId: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description The history, current record first */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["AssetValuationResponse"][];
                };
            };
            /** @description The asset identifier in the path is not a UUID */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description No access token, or it has expired or been revoked */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description No such asset, or it belongs to another user */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Wrong HTTP method. The history cannot be edited or deleted */
            405: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested media type cannot be produced */
            406: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    record: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                assetId: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["RecordValuationRequest"];
            };
        };
        responses: {
            /** @description The record was appended */
            201: {
                headers: {
                    /** @description The new record's URI, as /api/v1/assets/{assetId}/valuations/{valuationId}. */
                    Location?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["AssetValuationResponse"];
                };
            };
            /** @description A required field is missing, the value is negative or too precise, the currency is not the asset's, a date is in the future or before the purchase date, the rate snapshot is incomplete or on a VND record, or a correction names a record that is not this asset's or carries no note. An unrecognised currency is a malformed-request instead, because the body cannot be parsed. */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description No access token, or it has expired or been revoked */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description No such asset, it belongs to another user, or a concurrent request removed it first */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Wrong HTTP method. The history cannot be edited or deleted */
            405: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested media type cannot be produced */
            406: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The request body is not JSON */
            415: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    read_6: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description The checklist of the signed-in user */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["OnboardingResponse"];
                };
            };
            /** @description No access token, or it has expired or been revoked */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Wrong HTTP method */
            405: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested media type cannot be produced */
            406: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    recordProgress: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["UpdateOnboardingRequest"];
            };
        };
        responses: {
            /** @description The checklist after recording the progress */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["OnboardingResponse"];
                };
            };
            /** @description currentStep is outside 1 to 7 (validation-failed), or the body is not valid JSON, has an unknown status or a currentStep that is not a whole number (malformed-request) */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description No access token, or it has expired or been revoked */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Wrong HTTP method */
            405: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The request body is not JSON */
            415: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    meta: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description API metadata */
            200: {
                headers: {
                    /** @description Correlation identifier for this request */
                    "X-Correlation-Id"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["MetaResponse"];
                };
            };
            /** @description Wrong HTTP method */
            405: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested media type cannot be produced */
            406: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    list_5: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description The seven templates, in the backbone's order */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["GoalTemplateResponse"][];
                };
            };
            /** @description No access token, or it has expired or been revoked */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Wrong HTTP method; the templates are read-only */
            405: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested media type cannot be produced */
            406: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    monthly: {
        parameters: {
            query: {
                /** @description The calendar month to report, YYYY-MM. */
                month: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description The report, possibly of an empty month */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["MonthlyCashFlowReport"];
                };
            };
            /** @description month is missing, blank, or not a calendar month in the form YYYY-MM. */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description No access token, or it has expired or been revoked */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Wrong HTTP method */
            405: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested media type cannot be produced */
            406: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    list_6: {
        parameters: {
            query: {
                /** @description The first day of the window, included. */
                from: string;
                /** @description The last day of the window, included. Not before from, and less than 24 months after it. */
                to: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description The occurrences in the window, possibly none */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["CashFlowOccurrenceResponse"][];
                };
            };
            /** @description from or to is missing, blank, not a calendar date in the form YYYY-MM-DD, to is before from, or the window is 24 months or longer. Every problem is reported at once in the errors array. */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description No access token, or it has expired or been revoked */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Wrong HTTP method */
            405: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested media type cannot be produced */
            406: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    forecast: {
        parameters: {
            query?: {
                /** @description The first month, YYYY-MM. Defaults to the month after today; from that month to 12 months after the current one. */
                from?: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description The forecast. Always three months, possibly with no items. */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["CashFlowForecastResponse"];
                };
            };
            /** @description from is not a month in the form YYYY-MM, is before the month after today, or is more than 12 months after the current month. */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description No access token, or it has expired or been revoked */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Wrong HTTP method */
            405: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested media type cannot be produced */
            406: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    currentSession: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description The session is live */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["AuthenticatedUserResponse"];
                };
            };
            /** @description No access token, or it has expired or been revoked */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Wrong HTTP method */
            405: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested media type cannot be produced */
            406: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    read_7: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                assetId: string;
                valuationId: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description The record */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["AssetValuationResponse"];
                };
            };
            /** @description An identifier in the path is not a UUID */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description No access token, or it has expired or been revoked */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description No such asset or record, or the asset belongs to another user */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description Wrong HTTP method. A record is never edited or deleted */
            405: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
            /** @description The requested media type cannot be produced */
            406: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
}
