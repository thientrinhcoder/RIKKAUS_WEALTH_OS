/**
 * Generated from services/api/openapi/openapi.json by scripts/generate-api-contract.mjs.
 * Do not edit: run `npm run api:generate` after the contract changes.
 */

/** 51 examples, copied verbatim from the contract. */
export const contractExamples = {
  "GET /api/v1/settings/exchange-rate": {
    "200": {
      "contentType": "application/json",
      "examples": {
        "no rate declared yet": {
          "baseCurrency": "VND",
          "usdToVndRate": null,
          "effectiveDate": null,
          "updatedAt": null
        },
        "a declared rate": {
          "baseCurrency": "VND",
          "usdToVndRate": 25100,
          "effectiveDate": "2026-09-12",
          "updatedAt": "2026-09-12T02:42:11.503Z"
        }
      }
    }
  },
  "PUT /api/v1/settings/exchange-rate": {
    "400": {
      "contentType": "application/problem+json",
      "examples": {
        "effective date in the future": {
          "type": "urn:rikkaus:problem:validation-failed",
          "title": "Request validation failed",
          "status": 400,
          "detail": "Request validation failed.",
          "instance": "/api/v1/settings/exchange-rate",
          "correlationId": "b7f4c21a-9e03-4d55-8a16-2cf09b4d7e83",
          "errors": [
            {
              "field": "effectiveDate",
              "message": "must not be in the future"
            }
          ]
        }
      }
    }
  },
  "PUT /api/v1/profile": {
    "400": {
      "contentType": "application/problem+json",
      "examples": {
        "blank display name": {
          "type": "urn:rikkaus:problem:validation-failed",
          "title": "Request validation failed",
          "status": 400,
          "detail": "Request validation failed.",
          "instance": "/api/v1/profile",
          "correlationId": "b7f4c21a-9e03-4d55-8a16-2cf09b4d7e83",
          "errors": [
            {
              "field": "displayName",
              "message": "must not be blank"
            }
          ]
        }
      }
    }
  },
  "GET /api/v1/liabilities": {
    "200": {
      "contentType": "application/json",
      "examples": {
        "an empty catalog, or nothing matches the search": [],
        "a mortgage with collateral and a card balance": [
          {
            "id": "9d2f4c81-0a6e-4b73-b5c2-3e8f1d7a6c45",
            "name": "Thẻ tín dụng Techcombank",
            "category": "CREDIT_CARD_BALANCE",
            "outstandingBalance": 38500000,
            "currency": "VND",
            "interestRate": null,
            "startDate": null,
            "maturityDate": null,
            "recurringPaymentAmount": null,
            "recurringPaymentFrequency": null,
            "nextPaymentDate": "2026-11-25",
            "collateralAssetId": null,
            "notes": null,
            "createdAt": "2026-11-14T10:42:11.503Z",
            "updatedAt": "2026-11-14T10:42:11.503Z"
          },
          {
            "id": "5c0e2b7a-91d4-4f3e-8a6b-7d2c1e9f4a30",
            "name": "Vay mua nhà Vietcombank",
            "category": "BANK_LOAN",
            "outstandingBalance": 2400000000,
            "currency": "VND",
            "interestRate": 8.5,
            "startDate": "2024-03-15",
            "maturityDate": "2044-03-15",
            "recurringPaymentAmount": 23500000,
            "recurringPaymentFrequency": "MONTHLY",
            "nextPaymentDate": "2026-12-05",
            "collateralAssetId": "a81d4f27-6c3e-4b19-8f55-0d2e7c9a3b64",
            "notes": "Thế chấp căn hộ Thảo Điền.",
            "createdAt": "2026-11-14T10:40:02.117Z",
            "updatedAt": "2026-11-14T10:40:02.117Z"
          }
        ]
      }
    },
    "400": {
      "contentType": "application/problem+json",
      "examples": {
        "an unknown category": {
          "type": "urn:rikkaus:problem:validation-failed",
          "title": "Request validation failed",
          "status": 400,
          "detail": "Request validation failed.",
          "instance": "/api/v1/liabilities",
          "correlationId": "b7f4c21a-9e03-4d55-8a16-2cf09b4d7e83",
          "errors": [
            {
              "field": "category",
              "message": "must be one of BANK_LOAN, PERSONAL_LOAN, MARGIN_LOAN, CREDIT_CARD_BALANCE, GUARANTEE_OBLIGATION, OTHER_LIABILITY"
            }
          ]
        }
      }
    }
  },
  "POST /api/v1/liabilities": {
    "400": {
      "contentType": "application/problem+json",
      "examples": {
        "collateral the viewer does not own": {
          "type": "urn:rikkaus:problem:validation-failed",
          "title": "Request validation failed",
          "status": 400,
          "detail": "Request validation failed.",
          "instance": "/api/v1/liabilities",
          "correlationId": "b7f4c21a-9e03-4d55-8a16-2cf09b4d7e83",
          "errors": [
            {
              "field": "collateralAssetId",
              "message": "must identify an asset the signed-in user owns"
            }
          ]
        },
        "maturity before the start date": {
          "type": "urn:rikkaus:problem:validation-failed",
          "title": "Request validation failed",
          "status": 400,
          "detail": "Request validation failed.",
          "instance": "/api/v1/liabilities",
          "correlationId": "b7f4c21a-9e03-4d55-8a16-2cf09b4d7e83",
          "errors": [
            {
              "field": "maturityDate",
              "message": "must not be before the start date"
            }
          ]
        }
      }
    }
  },
  "GET /api/v1/goals": {
    "200": {
      "contentType": "application/json",
      "examples": {
        "no goals yet, or nothing matches the search": [],
        "two goals": [
          {
            "id": "5c2e7d31-9a4f-4b8e-a1d6-3f7b2c9e8a40",
            "name": "Quỹ dự phòng 6 tháng",
            "type": "EMERGENCY_FUND",
            "currency": "VND",
            "targetAmount": 270000000,
            "currentAllocatedAmount": 90000000,
            "targetDate": "2027-06-30",
            "plannedMonthlyContribution": 15000000,
            "expectedAnnualReturnPercent": 3,
            "createdAt": "2026-11-14T10:35:40.512Z",
            "updatedAt": "2026-11-14T10:35:40.512Z"
          },
          {
            "id": "9d4b1f68-2c7e-4a35-b8f0-6e1a5d3c7b92",
            "name": "Mua căn hộ",
            "type": "HOME_PURCHASE",
            "currency": "VND",
            "targetAmount": 3500000000,
            "currentAllocatedAmount": 1200000000,
            "targetDate": "2030-12-31",
            "plannedMonthlyContribution": 25000000,
            "expectedAnnualReturnPercent": 7,
            "createdAt": "2026-11-14T10:36:02.118Z",
            "updatedAt": "2026-11-14T10:36:02.118Z"
          }
        ]
      }
    },
    "400": {
      "contentType": "application/problem+json",
      "examples": {
        "an unknown type": {
          "type": "urn:rikkaus:problem:validation-failed",
          "title": "Request validation failed",
          "status": 400,
          "detail": "Request validation failed.",
          "instance": "/api/v1/goals",
          "correlationId": "b7f4c21a-9e03-4d55-8a16-2cf09b4d7e83",
          "errors": [
            {
              "field": "type",
              "message": "must be one of EMERGENCY_FUND, HOME_PURCHASE, EDUCATION, RETIREMENT, NET_WORTH_GROWTH, DEBT_REDUCTION, OTHER_GOAL"
            }
          ]
        }
      }
    }
  },
  "POST /api/v1/goals": {
    "400": {
      "contentType": "application/problem+json",
      "examples": {
        "target amount of zero": {
          "type": "urn:rikkaus:problem:validation-failed",
          "title": "Request validation failed",
          "status": 400,
          "detail": "Request validation failed.",
          "instance": "/api/v1/goals",
          "correlationId": "b7f4c21a-9e03-4d55-8a16-2cf09b4d7e83",
          "errors": [
            {
              "field": "targetAmount",
              "message": "must be greater than zero"
            }
          ]
        },
        "allocated more than the target": {
          "type": "urn:rikkaus:problem:validation-failed",
          "title": "Request validation failed",
          "status": 400,
          "detail": "Request validation failed.",
          "instance": "/api/v1/goals",
          "correlationId": "b7f4c21a-9e03-4d55-8a16-2cf09b4d7e83",
          "errors": [
            {
              "field": "currentAllocatedAmount",
              "message": "must not exceed the target amount"
            }
          ]
        },
        "target date not after today": {
          "type": "urn:rikkaus:problem:validation-failed",
          "title": "Request validation failed",
          "status": 400,
          "detail": "Request validation failed.",
          "instance": "/api/v1/goals",
          "correlationId": "b7f4c21a-9e03-4d55-8a16-2cf09b4d7e83",
          "errors": [
            {
              "field": "targetDate",
              "message": "must be after today"
            }
          ]
        }
      }
    }
  },
  "GET /api/v1/feedback": {
    "200": {
      "contentType": "application/json",
      "examples": {
        "no feedback yet": [],
        "a bug report and an uncategorised wish": [
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
      }
    }
  },
  "POST /api/v1/feedback": {
    "400": {
      "contentType": "application/problem+json",
      "examples": {
        "a sentence where the screen code belongs, and an empty message": {
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
      }
    }
  },
  "GET /api/v1/cash-flow-entries": {
    "200": {
      "contentType": "application/json",
      "examples": {
        "an empty catalog, or nothing matches the search": [],
        "a monthly salary, a quarterly tuition fee and a one-time USD dividend": [
          {
            "id": "5c0e2b7a-91d4-4f3b-8a6e-2d7f1c9b4e30",
            "entryType": "EXPENSE",
            "category": "EDUCATION",
            "amount": 12500000,
            "currency": "VND",
            "timing": "RECURRING",
            "recurrence": "QUARTERLY",
            "effectiveDate": "2026-09-05",
            "endDate": "2029-06-30",
            "linkedAssetId": null,
            "linkedLiabilityId": null,
            "notes": "Học phí",
            "createdAt": "2026-11-14T10:42:11.503Z",
            "updatedAt": "2026-11-14T10:42:11.503Z"
          },
          {
            "id": "e2a7c4d1-3b8f-4e6a-9c05-7d1b2f8a6c43",
            "entryType": "INCOME",
            "category": "DIVIDEND",
            "amount": 150,
            "currency": "USD",
            "timing": "ONE_TIME",
            "recurrence": null,
            "effectiveDate": "2026-09-12",
            "endDate": null,
            "linkedAssetId": "3f1c9a52-8d4b-4f0e-9c21-5b7a8e6d4f10",
            "linkedLiabilityId": null,
            "notes": null,
            "createdAt": "2026-11-14T10:40:02.117Z",
            "updatedAt": "2026-11-14T10:40:02.117Z"
          },
          {
            "id": "9b4d6f12-7e3a-4c58-b1d9-0a2e5c7f8d61",
            "entryType": "INCOME",
            "category": "SALARY",
            "amount": 45000000,
            "currency": "VND",
            "timing": "RECURRING",
            "recurrence": "MONTHLY",
            "effectiveDate": "2026-01-01",
            "endDate": null,
            "linkedAssetId": null,
            "linkedLiabilityId": null,
            "notes": "Lương tháng",
            "createdAt": "2026-11-14T10:38:45.902Z",
            "updatedAt": "2026-11-14T10:38:45.902Z"
          }
        ]
      }
    },
    "400": {
      "contentType": "application/problem+json",
      "examples": {
        "an unknown entry type and a reversed range": {
          "type": "urn:rikkaus:problem:validation-failed",
          "title": "Request validation failed",
          "status": 400,
          "detail": "Request validation failed.",
          "instance": "/api/v1/cash-flow-entries",
          "correlationId": "b7f4c21a-9e03-4d55-8a16-2cf09b4d7e83",
          "errors": [
            {
              "field": "entryType",
              "message": "must be one of INCOME, EXPENSE"
            },
            {
              "field": "effectiveTo",
              "message": "must not be before effectiveFrom"
            }
          ]
        }
      }
    }
  },
  "POST /api/v1/cash-flow-entries": {
    "400": {
      "contentType": "application/problem+json",
      "examples": {
        "category of the wrong entry type": {
          "type": "urn:rikkaus:problem:validation-failed",
          "title": "Request validation failed",
          "status": 400,
          "detail": "Request validation failed.",
          "instance": "/api/v1/cash-flow-entries",
          "correlationId": "b7f4c21a-9e03-4d55-8a16-2cf09b4d7e83",
          "errors": [
            {
              "field": "category",
              "message": "must belong to the entry type"
            }
          ]
        },
        "a one-time entry carrying a recurrence and an end date": {
          "type": "urn:rikkaus:problem:validation-failed",
          "title": "Request validation failed",
          "status": 400,
          "detail": "Request validation failed.",
          "instance": "/api/v1/cash-flow-entries",
          "correlationId": "b7f4c21a-9e03-4d55-8a16-2cf09b4d7e83",
          "errors": [
            {
              "field": "recurrence",
              "message": "must be omitted for a one-time entry"
            },
            {
              "field": "endDate",
              "message": "must be omitted for a one-time entry"
            }
          ]
        }
      }
    }
  },
  "POST /api/v1/auth/google": {
    "400": {
      "contentType": "application/problem+json",
      "examples": {
        "redirect URI not allowlisted": {
          "type": "urn:rikkaus:problem:validation-failed",
          "title": "Request validation failed",
          "status": 400,
          "detail": "Request validation failed.",
          "instance": "/api/v1/auth/google",
          "correlationId": "b7f4c21a-9e03-4d55-8a16-2cf09b4d7e83",
          "errors": [
            {
              "field": "redirectUri",
              "message": "is not an allowed redirect URI"
            }
          ]
        }
      }
    }
  },
  "GET /api/v1/assets": {
    "200": {
      "contentType": "application/json",
      "examples": {
        "an empty catalog, or nothing matches the search": [],
        "two assets, one in USD": [
          {
            "id": "3f1c9a52-8d4b-4f0e-9c21-5b7a8e6d4f10",
            "name": "Tài khoản USD",
            "category": "BANK_ACCOUNT",
            "currentValue": 20000,
            "currency": "USD",
            "valuationDate": "2026-09-12",
            "liquidity": "HIGH",
            "purchaseValue": null,
            "purchaseDate": null,
            "quantity": 20000,
            "institution": "Ngân hàng An Tín",
            "notes": "Tiền gửi dự phòng bằng USD.",
            "symbol": null,
            "costPerUnit": null,
            "currentPricePerUnit": null,
            "interestRate": null,
            "createdAt": "2026-09-12T02:42:11.503Z",
            "updatedAt": "2026-09-12T02:42:11.503Z"
          },
          {
            "id": "a81d4f27-6c3e-4b19-8f55-0d2e7c9a3b64",
            "name": "Căn hộ Thảo Điền",
            "category": "REAL_ESTATE",
            "currentValue": 6100000000,
            "currency": "VND",
            "valuationDate": "2026-08-02",
            "liquidity": "LOW",
            "purchaseValue": 4800000000,
            "purchaseDate": "2021-11-05",
            "quantity": null,
            "institution": "Sổ hồng cá nhân",
            "notes": null,
            "symbol": null,
            "costPerUnit": null,
            "currentPricePerUnit": null,
            "interestRate": null,
            "createdAt": "2026-08-02T01:15:03.221Z",
            "updatedAt": "2026-08-02T01:15:03.221Z"
          }
        ]
      }
    },
    "400": {
      "contentType": "application/problem+json",
      "examples": {
        "an unknown category and sort key": {
          "type": "urn:rikkaus:problem:validation-failed",
          "title": "Request validation failed",
          "status": 400,
          "detail": "Request validation failed.",
          "instance": "/api/v1/assets",
          "correlationId": "b7f4c21a-9e03-4d55-8a16-2cf09b4d7e83",
          "errors": [
            {
              "field": "category",
              "message": "must be one of CASH, BANK_ACCOUNT, TERM_DEPOSIT, LISTED_SECURITIES, BOND, INVESTMENT_FUND, REAL_ESTATE, PRIVATE_BUSINESS_EQUITY, VEHICLE_OR_VALUABLE_ASSET, COLLECTIBLE, OTHER_ASSET"
            },
            {
              "field": "sort",
              "message": "must be one of -valuationDate, valuationDate, name, -name"
            }
          ]
        }
      }
    }
  },
  "POST /api/v1/assets": {
    "201": {
      "contentType": "application/json",
      "examples": {
        "a listed holding, with the instrument fields": {
          "id": "c4e71b08-2a9d-4e63-8b17-91f0d35a7c82",
          "name": "Cổ phiếu VCB",
          "category": "LISTED_SECURITIES",
          "currentValue": 12000000,
          "currency": "VND",
          "valuationDate": "2026-11-10",
          "liquidity": "HIGH",
          "purchaseValue": null,
          "purchaseDate": "2025-04-07",
          "quantity": 500,
          "institution": "Chứng khoán An Tín",
          "notes": null,
          "symbol": "VCB",
          "costPerUnit": 21800,
          "currentPricePerUnit": 24150,
          "interestRate": null,
          "createdAt": "2026-11-10T01:44:12.118Z",
          "updatedAt": "2026-11-10T01:44:12.118Z"
        },
        "a term deposit, with a rate and nothing else": {
          "id": "5b2a9f31-7c84-4d06-9e51-3fa8c1b7d209",
          "name": "Tiền gửi 12 tháng An Tín",
          "category": "TERM_DEPOSIT",
          "currentValue": 300000000,
          "currency": "VND",
          "valuationDate": "2026-11-10",
          "liquidity": "LOW",
          "purchaseValue": null,
          "purchaseDate": null,
          "quantity": null,
          "institution": "Ngân hàng An Tín",
          "notes": null,
          "symbol": null,
          "costPerUnit": null,
          "currentPricePerUnit": null,
          "interestRate": 6.25,
          "createdAt": "2026-11-10T01:44:12.118Z",
          "updatedAt": "2026-11-10T01:44:12.118Z"
        }
      }
    },
    "400": {
      "contentType": "application/problem+json",
      "examples": {
        "liquidity missing": {
          "type": "urn:rikkaus:problem:validation-failed",
          "title": "Request validation failed",
          "status": 400,
          "detail": "Request validation failed.",
          "instance": "/api/v1/assets",
          "correlationId": "b7f4c21a-9e03-4d55-8a16-2cf09b4d7e83",
          "errors": [
            {
              "field": "liquidity",
              "message": "must not be null"
            }
          ]
        },
        "instrument fields the category does not accept": {
          "type": "urn:rikkaus:problem:validation-failed",
          "title": "Request validation failed",
          "status": 400,
          "detail": "Request validation failed.",
          "instance": "/api/v1/assets",
          "correlationId": "b7f4c21a-9e03-4d55-8a16-2cf09b4d7e83",
          "errors": [
            {
              "field": "symbol",
              "message": "must not be set for this asset category"
            },
            {
              "field": "currentPricePerUnit",
              "message": "must not be set for this asset category"
            }
          ]
        },
        "purchase date after the valuation date": {
          "type": "urn:rikkaus:problem:validation-failed",
          "title": "Request validation failed",
          "status": 400,
          "detail": "Request validation failed.",
          "instance": "/api/v1/assets",
          "correlationId": "b7f4c21a-9e03-4d55-8a16-2cf09b4d7e83",
          "errors": [
            {
              "field": "purchaseDate",
              "message": "must not be later than the valuation date"
            }
          ]
        }
      }
    }
  },
  "GET /api/v1/assets/{assetId}/valuations": {
    "200": {
      "contentType": "application/json",
      "examples": {
        "a USD account with a correction in its history": [
          {
            "id": "9c4e2b7a-1d35-4f68-a0c2-7e5b3d9f1a24",
            "assetId": "3f1c9a52-8d4b-4f0e-9c21-5b7a8e6d4f10",
            "value": 20000,
            "currency": "USD",
            "effectiveDate": "2026-09-12",
            "usdToVndRate": 25100,
            "rateEffectiveDate": "2026-09-12",
            "note": null,
            "correctsValuationId": null,
            "current": true,
            "recordedAt": "2026-09-12T02:42:11.503Z"
          },
          {
            "id": "e17b5c90-2f4a-4d83-b6e1-9c0d3f7a8b52",
            "assetId": "3f1c9a52-8d4b-4f0e-9c21-5b7a8e6d4f10",
            "value": 18500,
            "currency": "USD",
            "effectiveDate": "2026-05-31",
            "usdToVndRate": 25020,
            "rateEffectiveDate": "2026-05-30",
            "note": "Điều chỉnh số dư cuối tháng sau khi đối chiếu sao kê.",
            "correctsValuationId": "6d2f8a41-3b9c-4e07-8f15-2a7c4e9b0d63",
            "current": false,
            "recordedAt": "2026-06-03T08:20:05.117Z"
          },
          {
            "id": "6d2f8a41-3b9c-4e07-8f15-2a7c4e9b0d63",
            "assetId": "3f1c9a52-8d4b-4f0e-9c21-5b7a8e6d4f10",
            "value": 18000,
            "currency": "USD",
            "effectiveDate": "2026-05-31",
            "usdToVndRate": 25020,
            "rateEffectiveDate": "2026-05-30",
            "note": "Số dư cuối tháng được nhập ban đầu.",
            "correctsValuationId": null,
            "current": false,
            "recordedAt": "2026-05-31T13:02:48.330Z"
          }
        ]
      }
    }
  },
  "POST /api/v1/assets/{assetId}/valuations": {
    "201": {
      "contentType": "application/json",
      "examples": {
        "a newer value, now current": {
          "id": "4b8e1f36-7c2d-4a95-b0e3-1d6f9a2c5e78",
          "assetId": "3f1c9a52-8d4b-4f0e-9c21-5b7a8e6d4f10",
          "value": 21250,
          "currency": "USD",
          "effectiveDate": "2026-09-14",
          "usdToVndRate": 25100,
          "rateEffectiveDate": "2026-09-12",
          "note": "Cập nhật số dư sau khi đối chiếu sao kê.",
          "correctsValuationId": null,
          "current": true,
          "recordedAt": "2026-09-14T03:11:47.902Z"
        },
        "a correction of an older record, kept as history": {
          "id": "e17b5c90-2f4a-4d83-b6e1-9c0d3f7a8b52",
          "assetId": "3f1c9a52-8d4b-4f0e-9c21-5b7a8e6d4f10",
          "value": 18500,
          "currency": "USD",
          "effectiveDate": "2026-05-31",
          "usdToVndRate": 25020,
          "rateEffectiveDate": "2026-05-30",
          "note": "Điều chỉnh số dư cuối tháng sau khi đối chiếu sao kê.",
          "correctsValuationId": "6d2f8a41-3b9c-4e07-8f15-2a7c4e9b0d63",
          "current": false,
          "recordedAt": "2026-06-03T08:20:05.117Z"
        }
      }
    },
    "400": {
      "contentType": "application/problem+json",
      "examples": {
        "rate without its date": {
          "type": "urn:rikkaus:problem:validation-failed",
          "title": "Request validation failed",
          "status": 400,
          "detail": "Request validation failed.",
          "instance": "/api/v1/assets/3f1c9a52-8d4b-4f0e-9c21-5b7a8e6d4f10/valuations",
          "correlationId": "b7f4c21a-9e03-4d55-8a16-2cf09b4d7e83",
          "errors": [
            {
              "field": "rateEffectiveDate",
              "message": "must be given together with usdToVndRate"
            }
          ]
        },
        "correction without a reason": {
          "type": "urn:rikkaus:problem:validation-failed",
          "title": "Request validation failed",
          "status": 400,
          "detail": "Request validation failed.",
          "instance": "/api/v1/assets/3f1c9a52-8d4b-4f0e-9c21-5b7a8e6d4f10/valuations",
          "correlationId": "b7f4c21a-9e03-4d55-8a16-2cf09b4d7e83",
          "errors": [
            {
              "field": "note",
              "message": "must be given when correcting a valuation"
            }
          ]
        }
      }
    }
  },
  "GET /api/v1/onboarding": {
    "200": {
      "contentType": "application/json",
      "examples": {
        "a new user": {
          "status": "IN_PROGRESS",
          "currentStep": 1,
          "steps": [
            {
              "step": 1,
              "key": "EXCHANGE_RATE",
              "done": false
            },
            {
              "step": 2,
              "key": "CASH_AND_BANK_ACCOUNTS",
              "done": false
            },
            {
              "step": 3,
              "key": "MAJOR_ASSETS",
              "done": false
            },
            {
              "step": 4,
              "key": "LIABILITIES",
              "done": false
            },
            {
              "step": 5,
              "key": "RECURRING_CASH_FLOWS",
              "done": false
            },
            {
              "step": 6,
              "key": "DASHBOARD_REVIEW",
              "done": false
            },
            {
              "step": 7,
              "key": "FIRST_GOAL",
              "done": false
            }
          ],
          "dashboardReviewedAt": null,
          "completedAt": null,
          "dismissedAt": null
        },
        "rate, bank account and apartment added, viewing step 4": {
          "status": "IN_PROGRESS",
          "currentStep": 4,
          "steps": [
            {
              "step": 1,
              "key": "EXCHANGE_RATE",
              "done": true
            },
            {
              "step": 2,
              "key": "CASH_AND_BANK_ACCOUNTS",
              "done": true
            },
            {
              "step": 3,
              "key": "MAJOR_ASSETS",
              "done": true
            },
            {
              "step": 4,
              "key": "LIABILITIES",
              "done": false
            },
            {
              "step": 5,
              "key": "RECURRING_CASH_FLOWS",
              "done": false
            },
            {
              "step": 6,
              "key": "DASHBOARD_REVIEW",
              "done": false
            },
            {
              "step": 7,
              "key": "FIRST_GOAL",
              "done": false
            }
          ],
          "dashboardReviewedAt": null,
          "completedAt": null,
          "dismissedAt": null
        }
      }
    }
  },
  "PATCH /api/v1/onboarding": {
    "400": {
      "contentType": "application/problem+json",
      "examples": {
        "step outside the checklist": {
          "type": "urn:rikkaus:problem:validation-failed",
          "title": "Request validation failed",
          "status": 400,
          "detail": "Request validation failed.",
          "instance": "/api/v1/onboarding",
          "correlationId": "3d9a61c4-0b7e-4f28-9c53-e1a84f6b2d07",
          "errors": [
            {
              "field": "currentStep",
              "message": "must be between 1 and 7"
            }
          ]
        }
      }
    }
  },
  "GET /api/v1/meta": {
    "200": {
      "contentType": "application/json",
      "examples": {
        "current build": {
          "application": "rikkaus-wealth-api",
          "apiVersion": "v1",
          "buildVersion": "0.0.1-SNAPSHOT",
          "serverTime": "2026-09-27T07:42:00Z"
        }
      }
    }
  },
  "GET /api/v1/cash-flow-reports/monthly": {
    "200": {
      "contentType": "application/json",
      "examples": {
        "September 2026 after declaring 1 USD = 25,100 VND": {
          "month": "2026-09",
          "from": "2026-09-01",
          "to": "2026-09-30",
          "currency": "VND",
          "exchangeRate": {
            "usdToVndRate": 25100,
            "effectiveDate": "2026-09-12"
          },
          "complete": true,
          "totals": {
            "income": 43765000,
            "expense": 20020000,
            "net": 23745000
          },
          "unconverted": {
            "occurrenceCount": 0,
            "income": 0,
            "expense": 0
          },
          "incomeComposition": [
            {
              "category": "SALARY",
              "amount": 40000000,
              "sharePercent": 91.4,
              "occurrenceCount": 1,
              "partial": false
            },
            {
              "category": "DIVIDEND",
              "amount": 3765000,
              "sharePercent": 8.6,
              "occurrenceCount": 1,
              "partial": false
            }
          ],
          "expenseComposition": [
            {
              "category": "HOUSING",
              "amount": 15000000,
              "sharePercent": 74.9,
              "occurrenceCount": 1,
              "partial": false
            },
            {
              "category": "LIVING_COSTS",
              "amount": 5020000,
              "sharePercent": 25.1,
              "occurrenceCount": 1,
              "partial": false
            }
          ],
          "passiveIncome": {
            "total": 0,
            "sharePercentOfIncome": 0,
            "byAsset": [],
            "unassigned": {
              "amount": 3765000,
              "occurrenceCount": 1,
              "partial": false
            }
          },
          "occurrences": [
            {
              "sourceEntryId": "2e7c9a41-5b3d-4f86-a0c2-8d1e6b9f3a57",
              "occurrenceDate": "2026-09-01",
              "sequence": 1,
              "shiftedToMonthEnd": false,
              "entryType": "EXPENSE",
              "category": "HOUSING",
              "recurrence": null,
              "linkedAssetId": null,
              "notes": "Tiền nhà",
              "originalAmount": 15000000,
              "originalCurrency": "VND",
              "amount": 15000000
            },
            {
              "sourceEntryId": "8f14b6d2-3a9e-4c71-b5d8-0e2a7c4f9b13",
              "occurrenceDate": "2026-09-05",
              "sequence": 1,
              "shiftedToMonthEnd": false,
              "entryType": "INCOME",
              "category": "SALARY",
              "recurrence": null,
              "linkedAssetId": null,
              "notes": "Lương",
              "originalAmount": 40000000,
              "originalCurrency": "VND",
              "amount": 40000000
            },
            {
              "sourceEntryId": "c3a85e17-9d2b-4f40-8e6a-5b1f0d7c2e94",
              "occurrenceDate": "2026-09-10",
              "sequence": 1,
              "shiftedToMonthEnd": false,
              "entryType": "EXPENSE",
              "category": "LIVING_COSTS",
              "recurrence": null,
              "linkedAssetId": null,
              "notes": "Phần mềm làm việc",
              "originalAmount": 200,
              "originalCurrency": "USD",
              "amount": 5020000
            },
            {
              "sourceEntryId": "5d9e2f73-1c6a-4b85-9f30-a7e4c8b1d026",
              "occurrenceDate": "2026-09-30",
              "sequence": 1,
              "shiftedToMonthEnd": false,
              "entryType": "INCOME",
              "category": "DIVIDEND",
              "recurrence": null,
              "linkedAssetId": null,
              "notes": "Cổ tức ETF Mỹ",
              "originalAmount": 150,
              "originalCurrency": "USD",
              "amount": 3765000
            }
          ]
        },
        "the same month before any rate is declared": {
          "month": "2026-09",
          "from": "2026-09-01",
          "to": "2026-09-30",
          "currency": "VND",
          "exchangeRate": null,
          "complete": false,
          "totals": {
            "income": 40000000,
            "expense": 15000000,
            "net": 25000000
          },
          "unconverted": {
            "occurrenceCount": 2,
            "income": 150,
            "expense": 200
          },
          "incomeComposition": [
            {
              "category": "SALARY",
              "amount": 40000000,
              "sharePercent": 100,
              "occurrenceCount": 1,
              "partial": false
            },
            {
              "category": "DIVIDEND",
              "amount": 0,
              "sharePercent": 0,
              "occurrenceCount": 1,
              "partial": true
            }
          ],
          "expenseComposition": [
            {
              "category": "HOUSING",
              "amount": 15000000,
              "sharePercent": 100,
              "occurrenceCount": 1,
              "partial": false
            },
            {
              "category": "LIVING_COSTS",
              "amount": 0,
              "sharePercent": 0,
              "occurrenceCount": 1,
              "partial": true
            }
          ],
          "passiveIncome": {
            "total": 0,
            "sharePercentOfIncome": 0,
            "byAsset": [],
            "unassigned": {
              "amount": 0,
              "occurrenceCount": 1,
              "partial": true
            }
          },
          "occurrences": [
            {
              "sourceEntryId": "2e7c9a41-5b3d-4f86-a0c2-8d1e6b9f3a57",
              "occurrenceDate": "2026-09-01",
              "sequence": 1,
              "shiftedToMonthEnd": false,
              "entryType": "EXPENSE",
              "category": "HOUSING",
              "recurrence": null,
              "linkedAssetId": null,
              "notes": "Tiền nhà",
              "originalAmount": 15000000,
              "originalCurrency": "VND",
              "amount": 15000000
            },
            {
              "sourceEntryId": "8f14b6d2-3a9e-4c71-b5d8-0e2a7c4f9b13",
              "occurrenceDate": "2026-09-05",
              "sequence": 1,
              "shiftedToMonthEnd": false,
              "entryType": "INCOME",
              "category": "SALARY",
              "recurrence": null,
              "linkedAssetId": null,
              "notes": "Lương",
              "originalAmount": 40000000,
              "originalCurrency": "VND",
              "amount": 40000000
            },
            {
              "sourceEntryId": "c3a85e17-9d2b-4f40-8e6a-5b1f0d7c2e94",
              "occurrenceDate": "2026-09-10",
              "sequence": 1,
              "shiftedToMonthEnd": false,
              "entryType": "EXPENSE",
              "category": "LIVING_COSTS",
              "recurrence": null,
              "linkedAssetId": null,
              "notes": "Phần mềm làm việc",
              "originalAmount": 200,
              "originalCurrency": "USD",
              "amount": null
            },
            {
              "sourceEntryId": "5d9e2f73-1c6a-4b85-9f30-a7e4c8b1d026",
              "occurrenceDate": "2026-09-30",
              "sequence": 1,
              "shiftedToMonthEnd": false,
              "entryType": "INCOME",
              "category": "DIVIDEND",
              "recurrence": null,
              "linkedAssetId": null,
              "notes": "Cổ tức ETF Mỹ",
              "originalAmount": 150,
              "originalCurrency": "USD",
              "amount": null
            }
          ]
        },
        "a month with no occurrence": {
          "month": "2025-06",
          "from": "2025-06-01",
          "to": "2025-06-30",
          "currency": "VND",
          "exchangeRate": {
            "usdToVndRate": 25100,
            "effectiveDate": "2026-09-12"
          },
          "complete": true,
          "totals": {
            "income": 0,
            "expense": 0,
            "net": 0
          },
          "unconverted": {
            "occurrenceCount": 0,
            "income": 0,
            "expense": 0
          },
          "incomeComposition": [],
          "expenseComposition": [],
          "passiveIncome": {
            "total": 0,
            "sharePercentOfIncome": null,
            "byAsset": [],
            "unassigned": {
              "amount": 0,
              "occurrenceCount": 0,
              "partial": false
            }
          },
          "occurrences": []
        }
      }
    },
    "400": {
      "contentType": "application/problem+json",
      "examples": {
        "month not in the form YYYY-MM": {
          "type": "urn:rikkaus:problem:validation-failed",
          "title": "Request validation failed",
          "status": 400,
          "detail": "Request validation failed.",
          "instance": "/api/v1/cash-flow-reports/monthly",
          "correlationId": "b7f4c21a-9e03-4d55-8a16-2cf09b4d7e83",
          "errors": [
            {
              "field": "month",
              "message": "must be a calendar month in the form YYYY-MM"
            }
          ]
        }
      }
    }
  },
  "GET /api/v1/cash-flow-occurrences": {
    "200": {
      "contentType": "application/json",
      "examples": {
        "a window with no occurrence": [],
        "February 2026: a salary anchored on the 31st and a quarterly fee anchored on the 30th": [
          {
            "sourceEntryId": "5c0e2b7a-91d4-4f3b-8a6e-2d7f1c9b4e30",
            "occurrenceDate": "2026-02-28",
            "sequence": 2,
            "shiftedToMonthEnd": true,
            "entryType": "EXPENSE",
            "category": "EDUCATION",
            "amount": 12500000,
            "currency": "VND",
            "recurrence": "QUARTERLY"
          },
          {
            "sourceEntryId": "9b4d6f12-7e3a-4c58-b1d9-0a2e5c7f8d61",
            "occurrenceDate": "2026-02-28",
            "sequence": 2,
            "shiftedToMonthEnd": true,
            "entryType": "INCOME",
            "category": "SALARY",
            "amount": 45000000,
            "currency": "VND",
            "recurrence": "MONTHLY"
          }
        ]
      }
    },
    "400": {
      "contentType": "application/problem+json",
      "examples": {
        "to before from": {
          "type": "urn:rikkaus:problem:validation-failed",
          "title": "Request validation failed",
          "status": 400,
          "detail": "Request validation failed.",
          "instance": "/api/v1/cash-flow-occurrences",
          "correlationId": "b7f4c21a-9e03-4d55-8a16-2cf09b4d7e83",
          "errors": [
            {
              "field": "to",
              "message": "must not be before from"
            }
          ]
        },
        "from not a date and to missing": {
          "type": "urn:rikkaus:problem:validation-failed",
          "title": "Request validation failed",
          "status": 400,
          "detail": "Request validation failed.",
          "instance": "/api/v1/cash-flow-occurrences",
          "correlationId": "b7f4c21a-9e03-4d55-8a16-2cf09b4d7e83",
          "errors": [
            {
              "field": "from",
              "message": "must be a calendar date in the form YYYY-MM-DD"
            },
            {
              "field": "to",
              "message": "must be present"
            }
          ]
        }
      }
    }
  },
  "GET /api/v1/cash-flow-forecast": {
    "200": {
      "contentType": "application/json",
      "examples": {
        "a salary, a mortgage and a loan from family without a schedule": {
          "horizon": {
            "from": "2026-12-01",
            "to": "2027-02-28",
            "months": [
              "2026-12",
              "2027-01",
              "2027-02"
            ]
          },
          "openingBalance": {
            "amount": 85000000,
            "appliedOn": "2026-12-01",
            "sources": [
              {
                "sourceType": "ASSET",
                "sourceId": "3d7a51c2-8f04-4b6e-9a21-c5e0f8b2d419",
                "category": "BANK_ACCOUNT",
                "valuationDate": "2026-11-20",
                "amount": 85000000,
                "currency": "VND",
                "amountVnd": 85000000
              }
            ]
          },
          "months": [
            {
              "month": "2026-12",
              "income": 45000000,
              "expense": 18500000,
              "net": 26500000,
              "openingBalance": 85000000,
              "closingBalance": 111500000,
              "lowestBalance": 85000000,
              "lowestBalanceDate": "2026-12-01",
              "partial": false,
              "items": [
                {
                  "date": "2026-12-05",
                  "direction": "INCOME",
                  "sourceType": "CASH_FLOW_ENTRY",
                  "sourceId": "9b4d6f12-7e3a-4c58-b1d9-0a2e5c7f8d61",
                  "category": "SALARY",
                  "amount": 45000000,
                  "currency": "VND",
                  "amountVnd": 45000000,
                  "cadence": "MONTHLY",
                  "sequence": 12,
                  "shiftedToMonthEnd": false
                },
                {
                  "date": "2026-12-15",
                  "direction": "EXPENSE",
                  "sourceType": "LIABILITY",
                  "sourceId": "8e2f4a90-1c37-4d5b-b6a8-0f9d3e7c2b14",
                  "category": "BANK_LOAN",
                  "amount": 18500000,
                  "currency": "VND",
                  "amountVnd": 18500000,
                  "cadence": "MONTHLY",
                  "sequence": 1,
                  "shiftedToMonthEnd": false
                }
              ]
            },
            {
              "month": "2027-01",
              "income": 45000000,
              "expense": 18500000,
              "net": 26500000,
              "openingBalance": 111500000,
              "closingBalance": 138000000,
              "lowestBalance": 111500000,
              "lowestBalanceDate": "2027-01-01",
              "partial": true,
              "items": [
                {
                  "date": "2027-01-05",
                  "direction": "INCOME",
                  "sourceType": "CASH_FLOW_ENTRY",
                  "sourceId": "9b4d6f12-7e3a-4c58-b1d9-0a2e5c7f8d61",
                  "category": "SALARY",
                  "amount": 45000000,
                  "currency": "VND",
                  "amountVnd": 45000000,
                  "cadence": "MONTHLY",
                  "sequence": 13,
                  "shiftedToMonthEnd": false
                },
                {
                  "date": "2027-01-15",
                  "direction": "EXPENSE",
                  "sourceType": "LIABILITY",
                  "sourceId": "8e2f4a90-1c37-4d5b-b6a8-0f9d3e7c2b14",
                  "category": "BANK_LOAN",
                  "amount": 18500000,
                  "currency": "VND",
                  "amountVnd": 18500000,
                  "cadence": "MONTHLY",
                  "sequence": 2,
                  "shiftedToMonthEnd": false
                }
              ]
            },
            {
              "month": "2027-02",
              "income": 45000000,
              "expense": 18500000,
              "net": 26500000,
              "openingBalance": 138000000,
              "closingBalance": 164500000,
              "lowestBalance": 138000000,
              "lowestBalanceDate": "2027-02-01",
              "partial": false,
              "items": [
                {
                  "date": "2027-02-05",
                  "direction": "INCOME",
                  "sourceType": "CASH_FLOW_ENTRY",
                  "sourceId": "9b4d6f12-7e3a-4c58-b1d9-0a2e5c7f8d61",
                  "category": "SALARY",
                  "amount": 45000000,
                  "currency": "VND",
                  "amountVnd": 45000000,
                  "cadence": "MONTHLY",
                  "sequence": 14,
                  "shiftedToMonthEnd": false
                },
                {
                  "date": "2027-02-15",
                  "direction": "EXPENSE",
                  "sourceType": "LIABILITY",
                  "sourceId": "8e2f4a90-1c37-4d5b-b6a8-0f9d3e7c2b14",
                  "category": "BANK_LOAN",
                  "amount": 18500000,
                  "currency": "VND",
                  "amountVnd": 18500000,
                  "cadence": "MONTHLY",
                  "sequence": 3,
                  "shiftedToMonthEnd": false
                }
              ]
            }
          ],
          "minimumBalance": {
            "date": "2026-12-01",
            "month": "2026-12",
            "amount": 85000000,
            "negative": false,
            "referenceThreshold": 18500000,
            "status": "ADEQUATE"
          },
          "excludedSources": [
            {
              "sourceType": "LIABILITY",
              "sourceId": "c41b7e2d-6a95-4f08-8d3c-27e1b9f05a6e",
              "reason": "NO_PAYMENT_SCHEDULE",
              "affectedMonths": [
                "2027-01"
              ],
              "linkedEntryIds": []
            }
          ],
          "partial": true,
          "assumptions": {
            "asOfDate": "2026-11-22",
            "timeZone": "Asia/Ho_Chi_Minh",
            "currency": "VND",
            "exchangeRate": {
              "usdToVndRate": 25100,
              "effectiveDate": "2026-09-12"
            },
            "openingBalanceCategories": [
              "BANK_ACCOUNT",
              "CASH"
            ],
            "notProjected": {
              "from": "2026-11-23",
              "to": "2026-11-30"
            }
          }
        }
      }
    },
    "400": {
      "contentType": "application/problem+json",
      "examples": {
        "the current month": {
          "type": "urn:rikkaus:problem:validation-failed",
          "title": "Request validation failed",
          "status": 400,
          "detail": "Request validation failed.",
          "instance": "/api/v1/cash-flow-forecast",
          "correlationId": "b7f4c21a-9e03-4d55-8a16-2cf09b4d7e83",
          "errors": [
            {
              "field": "from",
              "message": "must not be before the month after today"
            }
          ]
        }
      }
    }
  }
} as const;
