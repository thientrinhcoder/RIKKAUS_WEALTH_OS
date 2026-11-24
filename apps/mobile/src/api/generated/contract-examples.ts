/**
 * Generated from services/api/openapi/openapi.json by scripts/generate-api-contract.mjs.
 * Do not edit: run `npm run api:generate` after the contract changes.
 */

/** 64 examples, copied verbatim from the contract. */
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
  "GET /api/v1/wealth-snapshot": {
    "200": {
      "contentType": "application/json",
      "examples": {
        "a USD account and a USD loan after declaring 1 USD = 25,100 VND": {
          "asOfDate": "2026-11-22",
          "timeZone": "Asia/Ho_Chi_Minh",
          "currency": "VND",
          "exchangeRate": {
            "usdToVndRate": 25100,
            "effectiveDate": "2026-11-20"
          },
          "complete": true,
          "kpis": {
            "totalAssets": {
              "amountVnd": 650000000,
              "knownAmountVnd": 650000000,
              "recordCount": 2,
              "excludedRecordCount": 0
            },
            "totalLiabilities": {
              "amountVnd": 125500000,
              "knownAmountVnd": 125500000,
              "recordCount": 1,
              "excludedRecordCount": 0
            },
            "netWorth": 524500000,
            "debtToAssetRatio": 0.193077,
            "liquidAssets": {
              "amountVnd": 650000000,
              "knownAmountVnd": 650000000,
              "recordCount": 2,
              "excludedRecordCount": 0
            },
            "lastDataUpdate": "2026-11-22"
          },
          "assetsByCategory": [
            {
              "category": "BANK_ACCOUNT",
              "amountVnd": 502000000,
              "knownAmountVnd": 502000000,
              "recordCount": 1,
              "excludedRecordCount": 0
            },
            {
              "category": "CASH",
              "amountVnd": 148000000,
              "knownAmountVnd": 148000000,
              "recordCount": 1,
              "excludedRecordCount": 0
            }
          ],
          "assetsByLiquidity": [
            {
              "liquidity": "HIGH",
              "amountVnd": 650000000,
              "knownAmountVnd": 650000000,
              "recordCount": 2,
              "excludedRecordCount": 0
            }
          ],
          "largestAssets": {
            "items": [
              {
                "assetId": "4b8d2e61-7a03-4f59-8c1e-d6a9b3f0e742",
                "name": "Tài khoản USD",
                "category": "BANK_ACCOUNT",
                "liquidity": "HIGH",
                "valueVnd": 502000000,
                "valuationDate": "2026-11-15"
              },
              {
                "assetId": "e3c17a9d-0f52-4b68-a4d1-9b6e2c8f5a03",
                "name": "Tiền mặt dự phòng",
                "category": "CASH",
                "liquidity": "HIGH",
                "valueVnd": 148000000,
                "valuationDate": "2026-11-15"
              }
            ],
            "excludedRecordCount": 0
          },
          "upcomingLiabilityDues": {
            "windowDays": 30,
            "windowEnd": "2026-12-22",
            "items": []
          },
          "sourceDates": [
            {
              "sourceType": "LIABILITY",
              "sourceId": "71d5b0e8-3c29-4a6f-8e17-2f9c4d6b1a85",
              "name": "Khoản vay USD từ người thân",
              "date": "2026-11-22"
            },
            {
              "sourceType": "EXCHANGE_RATE",
              "sourceId": null,
              "name": null,
              "date": "2026-11-20"
            },
            {
              "sourceType": "ASSET",
              "sourceId": "4b8d2e61-7a03-4f59-8c1e-d6a9b3f0e742",
              "name": "Tài khoản USD",
              "date": "2026-11-15"
            },
            {
              "sourceType": "ASSET",
              "sourceId": "e3c17a9d-0f52-4b68-a4d1-9b6e2c8f5a03",
              "name": "Tiền mặt dự phòng",
              "date": "2026-11-15"
            }
          ],
          "assets": [
            {
              "id": "4b8d2e61-7a03-4f59-8c1e-d6a9b3f0e742",
              "name": "Tài khoản USD",
              "category": "BANK_ACCOUNT",
              "liquidity": "HIGH",
              "value": 20000,
              "currency": "USD",
              "valueVnd": 502000000,
              "valuationDate": "2026-11-15"
            },
            {
              "id": "e3c17a9d-0f52-4b68-a4d1-9b6e2c8f5a03",
              "name": "Tiền mặt dự phòng",
              "category": "CASH",
              "liquidity": "HIGH",
              "value": 148000000,
              "currency": "VND",
              "valueVnd": 148000000,
              "valuationDate": "2026-11-15"
            }
          ],
          "liabilities": [
            {
              "id": "71d5b0e8-3c29-4a6f-8e17-2f9c4d6b1a85",
              "name": "Khoản vay USD từ người thân",
              "category": "PERSONAL_LOAN",
              "outstandingBalance": 5000,
              "currency": "USD",
              "outstandingBalanceVnd": 125500000,
              "nextPaymentDate": null,
              "maturityDate": null,
              "recurringPaymentAmount": null,
              "recurringPaymentFrequency": null,
              "updatedOn": "2026-11-22"
            }
          ]
        },
        "the same records before any rate is declared": {
          "asOfDate": "2026-11-22",
          "timeZone": "Asia/Ho_Chi_Minh",
          "currency": "VND",
          "exchangeRate": null,
          "complete": false,
          "kpis": {
            "totalAssets": {
              "amountVnd": null,
              "knownAmountVnd": 148000000,
              "recordCount": 2,
              "excludedRecordCount": 1
            },
            "totalLiabilities": {
              "amountVnd": null,
              "knownAmountVnd": 0,
              "recordCount": 1,
              "excludedRecordCount": 1
            },
            "netWorth": null,
            "debtToAssetRatio": null,
            "liquidAssets": {
              "amountVnd": null,
              "knownAmountVnd": 148000000,
              "recordCount": 2,
              "excludedRecordCount": 1
            },
            "lastDataUpdate": "2026-11-22"
          },
          "assetsByCategory": [
            {
              "category": "CASH",
              "amountVnd": 148000000,
              "knownAmountVnd": 148000000,
              "recordCount": 1,
              "excludedRecordCount": 0
            },
            {
              "category": "BANK_ACCOUNT",
              "amountVnd": null,
              "knownAmountVnd": 0,
              "recordCount": 1,
              "excludedRecordCount": 1
            }
          ],
          "assetsByLiquidity": [
            {
              "liquidity": "HIGH",
              "amountVnd": null,
              "knownAmountVnd": 148000000,
              "recordCount": 2,
              "excludedRecordCount": 1
            }
          ],
          "largestAssets": {
            "items": [
              {
                "assetId": "e3c17a9d-0f52-4b68-a4d1-9b6e2c8f5a03",
                "name": "Tiền mặt dự phòng",
                "category": "CASH",
                "liquidity": "HIGH",
                "valueVnd": 148000000,
                "valuationDate": "2026-11-15"
              }
            ],
            "excludedRecordCount": 1
          },
          "upcomingLiabilityDues": {
            "windowDays": 30,
            "windowEnd": "2026-12-22",
            "items": []
          },
          "sourceDates": [
            {
              "sourceType": "LIABILITY",
              "sourceId": "71d5b0e8-3c29-4a6f-8e17-2f9c4d6b1a85",
              "name": "Khoản vay USD từ người thân",
              "date": "2026-11-22"
            },
            {
              "sourceType": "ASSET",
              "sourceId": "4b8d2e61-7a03-4f59-8c1e-d6a9b3f0e742",
              "name": "Tài khoản USD",
              "date": "2026-11-15"
            },
            {
              "sourceType": "ASSET",
              "sourceId": "e3c17a9d-0f52-4b68-a4d1-9b6e2c8f5a03",
              "name": "Tiền mặt dự phòng",
              "date": "2026-11-15"
            }
          ],
          "assets": [
            {
              "id": "e3c17a9d-0f52-4b68-a4d1-9b6e2c8f5a03",
              "name": "Tiền mặt dự phòng",
              "category": "CASH",
              "liquidity": "HIGH",
              "value": 148000000,
              "currency": "VND",
              "valueVnd": 148000000,
              "valuationDate": "2026-11-15"
            },
            {
              "id": "4b8d2e61-7a03-4f59-8c1e-d6a9b3f0e742",
              "name": "Tài khoản USD",
              "category": "BANK_ACCOUNT",
              "liquidity": "HIGH",
              "value": 20000,
              "currency": "USD",
              "valueVnd": null,
              "valuationDate": "2026-11-15"
            }
          ],
          "liabilities": [
            {
              "id": "71d5b0e8-3c29-4a6f-8e17-2f9c4d6b1a85",
              "name": "Khoản vay USD từ người thân",
              "category": "PERSONAL_LOAN",
              "outstandingBalance": 5000,
              "currency": "USD",
              "outstandingBalanceVnd": null,
              "nextPaymentDate": null,
              "maturityDate": null,
              "recurringPaymentAmount": null,
              "recurringPaymentFrequency": null,
              "updatedOn": "2026-11-22"
            }
          ]
        },
        "a user with no record yet": {
          "asOfDate": "2026-11-22",
          "timeZone": "Asia/Ho_Chi_Minh",
          "currency": "VND",
          "exchangeRate": null,
          "complete": true,
          "kpis": {
            "totalAssets": {
              "amountVnd": 0,
              "knownAmountVnd": 0,
              "recordCount": 0,
              "excludedRecordCount": 0
            },
            "totalLiabilities": {
              "amountVnd": 0,
              "knownAmountVnd": 0,
              "recordCount": 0,
              "excludedRecordCount": 0
            },
            "netWorth": 0,
            "debtToAssetRatio": null,
            "liquidAssets": {
              "amountVnd": 0,
              "knownAmountVnd": 0,
              "recordCount": 0,
              "excludedRecordCount": 0
            },
            "lastDataUpdate": null
          },
          "assetsByCategory": [],
          "assetsByLiquidity": [],
          "largestAssets": {
            "items": [],
            "excludedRecordCount": 0
          },
          "upcomingLiabilityDues": {
            "windowDays": 30,
            "windowEnd": "2026-12-22",
            "items": []
          },
          "sourceDates": [],
          "assets": [],
          "liabilities": []
        }
      }
    }
  },
  "GET /api/v1/wealth-snapshot/insights": {
    "200": {
      "contentType": "application/json",
      "examples": {
        "the design fixture: 12,002,000,000 VND of assets, a stale rate": {
          "asOfDate": "2026-11-24",
          "timeZone": "Asia/Ho_Chi_Minh",
          "complete": true,
          "thresholds": {
            "maxCategoryShare": 0.5,
            "maxDebtToAssetRatio": 0.4,
            "maxLowLiquidityShare": 0.6,
            "dueWindowDays": 30,
            "maxValuationAgeDays": 30,
            "maxExchangeRateAgeDays": 5
          },
          "insights": [
            {
              "ruleId": "CATEGORY_CONCENTRATION",
              "status": "ATTENTION",
              "observed": {
                "value": 0.624896,
                "unit": "FRACTION",
                "subject": "REAL_ESTATE",
                "numeratorVnd": 7500000000,
                "denominatorVnd": 12002000000,
                "date": null
              },
              "threshold": {
                "comparison": "GREATER_THAN",
                "value": 0.5,
                "unit": "FRACTION"
              },
              "evidence": [
                {
                  "sourceType": "ASSET",
                  "sourceId": "d714c0bd-c311-4264-a7e6-453cb5254135",
                  "name": "Căn hộ Thảo Điền",
                  "category": "REAL_ESTATE",
                  "liquidity": "LOW",
                  "dateKind": "VALUATION_DATE",
                  "date": "2026-10-07",
                  "ageDays": 48,
                  "daysUntilDue": null,
                  "overdue": null,
                  "amount": 7500000000,
                  "currency": "VND",
                  "amountVnd": 7500000000,
                  "usdToVndRate": null
                }
              ],
              "unavailableReason": null
            },
            {
              "ruleId": "DEBT_TO_ASSET_RATIO",
              "status": "OK",
              "observed": {
                "value": 0.127645,
                "unit": "FRACTION",
                "subject": null,
                "numeratorVnd": 1532000000,
                "denominatorVnd": 12002000000,
                "date": null
              },
              "threshold": {
                "comparison": "GREATER_THAN",
                "value": 0.4,
                "unit": "FRACTION"
              },
              "evidence": [
                {
                  "sourceType": "LIABILITY",
                  "sourceId": "a16f71f4-416f-44d0-94ef-19d56449426d",
                  "name": "Khoản vay mua căn hộ",
                  "category": "BANK_LOAN",
                  "liquidity": null,
                  "dateKind": "UPDATED_ON",
                  "date": "2026-11-24",
                  "ageDays": 0,
                  "daysUntilDue": null,
                  "overdue": null,
                  "amount": 1500000000,
                  "currency": "VND",
                  "amountVnd": 1500000000,
                  "usdToVndRate": null
                },
                {
                  "sourceType": "LIABILITY",
                  "sourceId": "ecdd66f2-ddd6-432f-8a88-79e6e39bc943",
                  "name": "Dư nợ thẻ tín dụng",
                  "category": "CREDIT_CARD_BALANCE",
                  "liquidity": null,
                  "dateKind": "UPDATED_ON",
                  "date": "2026-11-24",
                  "ageDays": 0,
                  "daysUntilDue": null,
                  "overdue": null,
                  "amount": 32000000,
                  "currency": "VND",
                  "amountVnd": 32000000,
                  "usdToVndRate": null
                }
              ],
              "unavailableReason": null
            },
            {
              "ruleId": "LOW_LIQUIDITY_SHARE",
              "status": "ATTENTION",
              "observed": {
                "value": 0.624896,
                "unit": "FRACTION",
                "subject": null,
                "numeratorVnd": 7500000000,
                "denominatorVnd": 12002000000,
                "date": null
              },
              "threshold": {
                "comparison": "GREATER_THAN",
                "value": 0.6,
                "unit": "FRACTION"
              },
              "evidence": [
                {
                  "sourceType": "ASSET",
                  "sourceId": "d714c0bd-c311-4264-a7e6-453cb5254135",
                  "name": "Căn hộ Thảo Điền",
                  "category": "REAL_ESTATE",
                  "liquidity": "LOW",
                  "dateKind": "VALUATION_DATE",
                  "date": "2026-10-07",
                  "ageDays": 48,
                  "daysUntilDue": null,
                  "overdue": null,
                  "amount": 7500000000,
                  "currency": "VND",
                  "amountVnd": 7500000000,
                  "usdToVndRate": null
                }
              ],
              "unavailableReason": null
            },
            {
              "ruleId": "UPCOMING_LIABILITY_DUE",
              "status": "ATTENTION",
              "observed": {
                "value": 12,
                "unit": "DAYS",
                "subject": null,
                "numeratorVnd": null,
                "denominatorVnd": null,
                "date": "2026-12-06"
              },
              "threshold": {
                "comparison": "AT_MOST",
                "value": 30,
                "unit": "DAYS"
              },
              "evidence": [
                {
                  "sourceType": "LIABILITY",
                  "sourceId": "a16f71f4-416f-44d0-94ef-19d56449426d",
                  "name": "Khoản vay mua căn hộ",
                  "category": "BANK_LOAN",
                  "liquidity": null,
                  "dateKind": "NEXT_PAYMENT_DATE",
                  "date": "2026-12-06",
                  "ageDays": null,
                  "daysUntilDue": 12,
                  "overdue": false,
                  "amount": 18750000,
                  "currency": "VND",
                  "amountVnd": 18750000,
                  "usdToVndRate": null
                }
              ],
              "unavailableReason": null
            },
            {
              "ruleId": "STALE_VALUATION",
              "status": "ATTENTION",
              "observed": {
                "value": 48,
                "unit": "DAYS",
                "subject": null,
                "numeratorVnd": null,
                "denominatorVnd": null,
                "date": "2026-10-07"
              },
              "threshold": {
                "comparison": "GREATER_THAN",
                "value": 30,
                "unit": "DAYS"
              },
              "evidence": [
                {
                  "sourceType": "ASSET",
                  "sourceId": "d714c0bd-c311-4264-a7e6-453cb5254135",
                  "name": "Căn hộ Thảo Điền",
                  "category": "REAL_ESTATE",
                  "liquidity": "LOW",
                  "dateKind": "VALUATION_DATE",
                  "date": "2026-10-07",
                  "ageDays": 48,
                  "daysUntilDue": null,
                  "overdue": null,
                  "amount": 7500000000,
                  "currency": "VND",
                  "amountVnd": 7500000000,
                  "usdToVndRate": null
                }
              ],
              "unavailableReason": null
            },
            {
              "ruleId": "STALE_EXCHANGE_RATE",
              "status": "ATTENTION",
              "observed": {
                "value": 73,
                "unit": "DAYS",
                "subject": null,
                "numeratorVnd": null,
                "denominatorVnd": null,
                "date": "2026-09-12"
              },
              "threshold": {
                "comparison": "GREATER_THAN",
                "value": 5,
                "unit": "DAYS"
              },
              "evidence": [
                {
                  "sourceType": "EXCHANGE_RATE",
                  "sourceId": null,
                  "name": null,
                  "category": null,
                  "liquidity": null,
                  "dateKind": "RATE_EFFECTIVE_DATE",
                  "date": "2026-09-12",
                  "ageDays": 73,
                  "daysUntilDue": null,
                  "overdue": null,
                  "amount": null,
                  "currency": null,
                  "amountVnd": null,
                  "usdToVndRate": 25100
                },
                {
                  "sourceType": "ASSET",
                  "sourceId": "5590ab36-5bf7-4248-8ddc-b9abb41a11f7",
                  "name": "Tiền gửi USD",
                  "category": "BANK_ACCOUNT",
                  "liquidity": "HIGH",
                  "dateKind": "VALUATION_DATE",
                  "date": "2026-11-19",
                  "ageDays": 5,
                  "daysUntilDue": null,
                  "overdue": null,
                  "amount": 20000,
                  "currency": "USD",
                  "amountVnd": 502000000,
                  "usdToVndRate": null
                }
              ],
              "unavailableReason": null
            }
          ]
        },
        "a USD account and a USD loan before any rate is declared": {
          "asOfDate": "2026-11-24",
          "timeZone": "Asia/Ho_Chi_Minh",
          "complete": false,
          "thresholds": {
            "maxCategoryShare": 0.5,
            "maxDebtToAssetRatio": 0.4,
            "maxLowLiquidityShare": 0.6,
            "dueWindowDays": 30,
            "maxValuationAgeDays": 30,
            "maxExchangeRateAgeDays": 5
          },
          "insights": [
            {
              "ruleId": "CATEGORY_CONCENTRATION",
              "status": "UNAVAILABLE",
              "observed": null,
              "threshold": {
                "comparison": "GREATER_THAN",
                "value": 0.5,
                "unit": "FRACTION"
              },
              "evidence": [
                {
                  "sourceType": "ASSET",
                  "sourceId": "3897dd94-10c9-467d-9998-53dbb6429778",
                  "name": "Tài khoản USD",
                  "category": "BANK_ACCOUNT",
                  "liquidity": "HIGH",
                  "dateKind": "VALUATION_DATE",
                  "date": "2026-11-21",
                  "ageDays": 3,
                  "daysUntilDue": null,
                  "overdue": null,
                  "amount": 20000,
                  "currency": "USD",
                  "amountVnd": null,
                  "usdToVndRate": null
                }
              ],
              "unavailableReason": "MISSING_EXCHANGE_RATE"
            },
            {
              "ruleId": "DEBT_TO_ASSET_RATIO",
              "status": "UNAVAILABLE",
              "observed": null,
              "threshold": {
                "comparison": "GREATER_THAN",
                "value": 0.4,
                "unit": "FRACTION"
              },
              "evidence": [
                {
                  "sourceType": "ASSET",
                  "sourceId": "3897dd94-10c9-467d-9998-53dbb6429778",
                  "name": "Tài khoản USD",
                  "category": "BANK_ACCOUNT",
                  "liquidity": "HIGH",
                  "dateKind": "VALUATION_DATE",
                  "date": "2026-11-21",
                  "ageDays": 3,
                  "daysUntilDue": null,
                  "overdue": null,
                  "amount": 20000,
                  "currency": "USD",
                  "amountVnd": null,
                  "usdToVndRate": null
                },
                {
                  "sourceType": "LIABILITY",
                  "sourceId": "5d55a3cc-4211-4e0c-bcb0-e9b018f99e91",
                  "name": "Khoản vay USD từ người thân",
                  "category": "PERSONAL_LOAN",
                  "liquidity": null,
                  "dateKind": "UPDATED_ON",
                  "date": "2026-11-24",
                  "ageDays": 0,
                  "daysUntilDue": null,
                  "overdue": null,
                  "amount": 5000,
                  "currency": "USD",
                  "amountVnd": null,
                  "usdToVndRate": null
                }
              ],
              "unavailableReason": "MISSING_EXCHANGE_RATE"
            },
            {
              "ruleId": "LOW_LIQUIDITY_SHARE",
              "status": "UNAVAILABLE",
              "observed": null,
              "threshold": {
                "comparison": "GREATER_THAN",
                "value": 0.6,
                "unit": "FRACTION"
              },
              "evidence": [
                {
                  "sourceType": "ASSET",
                  "sourceId": "3897dd94-10c9-467d-9998-53dbb6429778",
                  "name": "Tài khoản USD",
                  "category": "BANK_ACCOUNT",
                  "liquidity": "HIGH",
                  "dateKind": "VALUATION_DATE",
                  "date": "2026-11-21",
                  "ageDays": 3,
                  "daysUntilDue": null,
                  "overdue": null,
                  "amount": 20000,
                  "currency": "USD",
                  "amountVnd": null,
                  "usdToVndRate": null
                }
              ],
              "unavailableReason": "MISSING_EXCHANGE_RATE"
            },
            {
              "ruleId": "UPCOMING_LIABILITY_DUE",
              "status": "ATTENTION",
              "observed": {
                "value": 20,
                "unit": "DAYS",
                "subject": null,
                "numeratorVnd": null,
                "denominatorVnd": null,
                "date": "2026-12-14"
              },
              "threshold": {
                "comparison": "AT_MOST",
                "value": 30,
                "unit": "DAYS"
              },
              "evidence": [
                {
                  "sourceType": "LIABILITY",
                  "sourceId": "5d55a3cc-4211-4e0c-bcb0-e9b018f99e91",
                  "name": "Khoản vay USD từ người thân",
                  "category": "PERSONAL_LOAN",
                  "liquidity": null,
                  "dateKind": "MATURITY_DATE",
                  "date": "2026-12-14",
                  "ageDays": null,
                  "daysUntilDue": 20,
                  "overdue": false,
                  "amount": 5000,
                  "currency": "USD",
                  "amountVnd": null,
                  "usdToVndRate": null
                }
              ],
              "unavailableReason": null
            },
            {
              "ruleId": "STALE_VALUATION",
              "status": "OK",
              "observed": {
                "value": 3,
                "unit": "DAYS",
                "subject": null,
                "numeratorVnd": null,
                "denominatorVnd": null,
                "date": "2026-11-21"
              },
              "threshold": {
                "comparison": "GREATER_THAN",
                "value": 30,
                "unit": "DAYS"
              },
              "evidence": [
                {
                  "sourceType": "ASSET",
                  "sourceId": "3897dd94-10c9-467d-9998-53dbb6429778",
                  "name": "Tài khoản USD",
                  "category": "BANK_ACCOUNT",
                  "liquidity": "HIGH",
                  "dateKind": "VALUATION_DATE",
                  "date": "2026-11-21",
                  "ageDays": 3,
                  "daysUntilDue": null,
                  "overdue": null,
                  "amount": 20000,
                  "currency": "USD",
                  "amountVnd": null,
                  "usdToVndRate": null
                }
              ],
              "unavailableReason": null
            },
            {
              "ruleId": "STALE_EXCHANGE_RATE",
              "status": "UNAVAILABLE",
              "observed": null,
              "threshold": {
                "comparison": "GREATER_THAN",
                "value": 5,
                "unit": "DAYS"
              },
              "evidence": [
                {
                  "sourceType": "ASSET",
                  "sourceId": "3897dd94-10c9-467d-9998-53dbb6429778",
                  "name": "Tài khoản USD",
                  "category": "BANK_ACCOUNT",
                  "liquidity": "HIGH",
                  "dateKind": "VALUATION_DATE",
                  "date": "2026-11-21",
                  "ageDays": 3,
                  "daysUntilDue": null,
                  "overdue": null,
                  "amount": 20000,
                  "currency": "USD",
                  "amountVnd": null,
                  "usdToVndRate": null
                },
                {
                  "sourceType": "LIABILITY",
                  "sourceId": "5d55a3cc-4211-4e0c-bcb0-e9b018f99e91",
                  "name": "Khoản vay USD từ người thân",
                  "category": "PERSONAL_LOAN",
                  "liquidity": null,
                  "dateKind": "UPDATED_ON",
                  "date": "2026-11-24",
                  "ageDays": 0,
                  "daysUntilDue": null,
                  "overdue": null,
                  "amount": 5000,
                  "currency": "USD",
                  "amountVnd": null,
                  "usdToVndRate": null
                }
              ],
              "unavailableReason": "MISSING_EXCHANGE_RATE"
            }
          ]
        },
        "a user with no record yet": {
          "asOfDate": "2026-11-24",
          "timeZone": "Asia/Ho_Chi_Minh",
          "complete": true,
          "thresholds": {
            "maxCategoryShare": 0.5,
            "maxDebtToAssetRatio": 0.4,
            "maxLowLiquidityShare": 0.6,
            "dueWindowDays": 30,
            "maxValuationAgeDays": 30,
            "maxExchangeRateAgeDays": 5
          },
          "insights": [
            {
              "ruleId": "CATEGORY_CONCENTRATION",
              "status": "UNAVAILABLE",
              "observed": null,
              "threshold": {
                "comparison": "GREATER_THAN",
                "value": 0.5,
                "unit": "FRACTION"
              },
              "evidence": [],
              "unavailableReason": "NO_ASSETS"
            },
            {
              "ruleId": "DEBT_TO_ASSET_RATIO",
              "status": "UNAVAILABLE",
              "observed": null,
              "threshold": {
                "comparison": "GREATER_THAN",
                "value": 0.4,
                "unit": "FRACTION"
              },
              "evidence": [],
              "unavailableReason": "NO_ASSETS"
            },
            {
              "ruleId": "LOW_LIQUIDITY_SHARE",
              "status": "UNAVAILABLE",
              "observed": null,
              "threshold": {
                "comparison": "GREATER_THAN",
                "value": 0.6,
                "unit": "FRACTION"
              },
              "evidence": [],
              "unavailableReason": "NO_ASSETS"
            },
            {
              "ruleId": "UPCOMING_LIABILITY_DUE",
              "status": "UNAVAILABLE",
              "observed": null,
              "threshold": {
                "comparison": "AT_MOST",
                "value": 30,
                "unit": "DAYS"
              },
              "evidence": [],
              "unavailableReason": "NO_LIABILITIES"
            },
            {
              "ruleId": "STALE_VALUATION",
              "status": "UNAVAILABLE",
              "observed": null,
              "threshold": {
                "comparison": "GREATER_THAN",
                "value": 30,
                "unit": "DAYS"
              },
              "evidence": [],
              "unavailableReason": "NO_ASSETS"
            },
            {
              "ruleId": "STALE_EXCHANGE_RATE",
              "status": "UNAVAILABLE",
              "observed": null,
              "threshold": {
                "comparison": "GREATER_THAN",
                "value": 5,
                "unit": "DAYS"
              },
              "evidence": [],
              "unavailableReason": "MISSING_EXCHANGE_RATE"
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
  "GET /api/v1/liabilities/{liabilityId}/deletion-impact": {
    "200": {
      "contentType": "application/json",
      "examples": {
        "Mortgage with its monthly instalment linked": {
          "recordType": "LIABILITY",
          "recordId": "9d1e7c42-5b6a-4f3e-8d2c-1b0a9f8e7d6c",
          "confirmationRequired": true,
          "linkedEffects": [
            {
              "recordType": "CASH_FLOW_ENTRY",
              "effect": "UNLINKED",
              "field": "linkedLiabilityId",
              "count": 1,
              "records": [
                {
                  "id": "5e7a9c1b-3d4f-4a6b-8c0d-2e4f6a8b0c1d",
                  "label": "Trả góp vay mua nhà Vietcombank",
                  "category": "DEBT_PRINCIPAL_PAYMENT"
                }
              ]
            }
          ],
          "derivedEffects": []
        }
      }
    }
  },
  "GET /api/v1/goal-projections": {
    "200": {
      "contentType": "application/json",
      "examples": {
        "no goals yet": [],
        "one goal whose target date has passed": [
          {
            "goalId": "c3e8a1f5-7b24-4d96-8e0a-1f5c9b3d6e27",
            "goalName": "Đổi xe gia đình",
            "currency": "VND",
            "asOfDate": "2026-11-22",
            "timeline": "EXPIRED",
            "targetReached": false,
            "completionPercent": 77.5,
            "remainingAmount": 180000000,
            "remainingMonths": 0,
            "daysPastTargetDate": 22,
            "requiredMonthlyContribution": null,
            "contributionCoveragePercent": null,
            "status": null,
            "projectedAtTargetDate": null,
            "calculation": null,
            "unavailableReasons": [
              "TARGET_DATE_PASSED"
            ],
            "assumptions": [
              {
                "key": "TARGET_AMOUNT",
                "source": "USER_ENTERED",
                "value": "800000000.00"
              },
              {
                "key": "CURRENT_ALLOCATED_AMOUNT",
                "source": "USER_ENTERED",
                "value": "620000000.00"
              },
              {
                "key": "TARGET_DATE",
                "source": "USER_ENTERED",
                "value": "2026-10-31"
              },
              {
                "key": "PLANNED_MONTHLY_CONTRIBUTION",
                "source": "USER_ENTERED",
                "value": "0.00"
              },
              {
                "key": "EXPECTED_ANNUAL_RETURN_PERCENT",
                "source": "USER_ENTERED",
                "value": "4.00"
              },
              {
                "key": "AS_OF_DATE",
                "source": "SYSTEM",
                "value": "2026-11-22"
              },
              {
                "key": "TIME_ZONE",
                "source": "SYSTEM",
                "value": "Asia/Ho_Chi_Minh"
              },
              {
                "key": "FIRST_CONTRIBUTION_MONTH",
                "source": "SYSTEM",
                "value": null
              },
              {
                "key": "COMPOUNDING",
                "source": "SYSTEM",
                "value": "MONTHLY"
              },
              {
                "key": "CONTRIBUTION_TIMING",
                "source": "SYSTEM",
                "value": "END_OF_MONTH"
              },
              {
                "key": "REQUIRED_CONTRIBUTION_ROUNDING",
                "source": "SYSTEM",
                "value": "UP_TO_1000_VND"
              },
              {
                "key": "ON_TRACK_MIN_COVERAGE_PERCENT",
                "source": "SYSTEM",
                "value": "100"
              },
              {
                "key": "AT_RISK_MIN_COVERAGE_PERCENT",
                "source": "SYSTEM",
                "value": "80"
              },
              {
                "key": "INFLATION",
                "source": "NOT_MODELLED",
                "value": null
              },
              {
                "key": "TAXES_AND_FEES",
                "source": "NOT_MODELLED",
                "value": null
              },
              {
                "key": "RETURN_VARIABILITY",
                "source": "NOT_MODELLED",
                "value": null
              },
              {
                "key": "CASH_FLOW_LINK",
                "source": "NOT_MODELLED",
                "value": null
              }
            ],
            "method": {
              "name": "FUTURE_VALUE_MONTHLY_COMPOUNDING",
              "remainingMonths": "n = calendar months from the month after asOfDate through the month of targetDate, both included; contributions arrive at the end of each month. A target date before asOfDate is EXPIRED; a target date later this month leaves n = 0 and is DUE_THIS_MONTH.",
              "monthlyRate": "r = expectedAnnualReturnPercent / 100 / 12, compounded monthly; g = (1 + r)^n.",
              "projectedAmount": "fromAllocated = currentAllocatedAmount × g; fromContributions = plannedMonthlyContribution × (g − 1) / r, or plannedMonthlyContribution × n when r = 0; total = fromAllocated + fromContributions; gapToTarget = total − targetAmount.",
              "requiredMonthlyContribution": "(targetAmount − currentAllocatedAmount × g) × r / (g − 1), or (targetAmount − currentAllocatedAmount) / n when r = 0; 0 when the goal is already funded or the allocated amount alone reaches the target; otherwise rounded up to the next 1,000 VND.",
              "status": "contributionCoveragePercent = plannedMonthlyContribution / requiredMonthlyContribution × 100 (100 when nothing is required). ON_TRACK at or above 100, AT_RISK at or above 80, BEHIND below; these are system reference thresholds, compared before the percentage is rounded.",
              "rounding": "Intermediate values carry 34 significant digits. Each part of the projected amount is rounded half up to 2 decimals before they are added, so the parts add up to the total. Percentages are rounded down to 2 decimals."
            },
            "disclaimer": "The expected annual return is the user's own assumption, not a guaranteed, promised or predicted return. Projected amounts illustrate what the entered figures would produce if that return were earned every month; actual results will differ."
          }
        ]
      }
    }
  },
  "GET /api/v1/goal-projections/{goalId}": {
    "200": {
      "contentType": "application/json",
      "examples": {
        "an active goal on track": {
          "goalId": "9d4b1f68-2c7e-4a35-b8f0-6e1a5d3c7b92",
          "goalName": "Mua nhà cho con",
          "currency": "VND",
          "asOfDate": "2026-11-22",
          "timeline": "ACTIVE",
          "targetReached": false,
          "completionPercent": 40,
          "remainingAmount": 1800000000,
          "remainingMonths": 57,
          "daysPastTargetDate": null,
          "requiredMonthlyContribution": 21371000,
          "contributionCoveragePercent": 116.98,
          "status": "ON_TRACK",
          "projectedAtTargetDate": {
            "fromAllocated": 1594581665.13,
            "fromContributions": 1644090271.39,
            "total": 3238671936.52,
            "gapToTarget": 238671936.52
          },
          "calculation": {
            "monthlyRatePercent": 0.5,
            "growthFactor": 1.3288180543
          },
          "unavailableReasons": [],
          "assumptions": [
            {
              "key": "TARGET_AMOUNT",
              "source": "USER_ENTERED",
              "value": "3000000000.00"
            },
            {
              "key": "CURRENT_ALLOCATED_AMOUNT",
              "source": "USER_ENTERED",
              "value": "1200000000.00"
            },
            {
              "key": "TARGET_DATE",
              "source": "USER_ENTERED",
              "value": "2031-08-31"
            },
            {
              "key": "PLANNED_MONTHLY_CONTRIBUTION",
              "source": "USER_ENTERED",
              "value": "25000000.00"
            },
            {
              "key": "EXPECTED_ANNUAL_RETURN_PERCENT",
              "source": "USER_ENTERED",
              "value": "6.00"
            },
            {
              "key": "AS_OF_DATE",
              "source": "SYSTEM",
              "value": "2026-11-22"
            },
            {
              "key": "TIME_ZONE",
              "source": "SYSTEM",
              "value": "Asia/Ho_Chi_Minh"
            },
            {
              "key": "FIRST_CONTRIBUTION_MONTH",
              "source": "SYSTEM",
              "value": "2026-12"
            },
            {
              "key": "COMPOUNDING",
              "source": "SYSTEM",
              "value": "MONTHLY"
            },
            {
              "key": "CONTRIBUTION_TIMING",
              "source": "SYSTEM",
              "value": "END_OF_MONTH"
            },
            {
              "key": "REQUIRED_CONTRIBUTION_ROUNDING",
              "source": "SYSTEM",
              "value": "UP_TO_1000_VND"
            },
            {
              "key": "ON_TRACK_MIN_COVERAGE_PERCENT",
              "source": "SYSTEM",
              "value": "100"
            },
            {
              "key": "AT_RISK_MIN_COVERAGE_PERCENT",
              "source": "SYSTEM",
              "value": "80"
            },
            {
              "key": "INFLATION",
              "source": "NOT_MODELLED",
              "value": null
            },
            {
              "key": "TAXES_AND_FEES",
              "source": "NOT_MODELLED",
              "value": null
            },
            {
              "key": "RETURN_VARIABILITY",
              "source": "NOT_MODELLED",
              "value": null
            },
            {
              "key": "CASH_FLOW_LINK",
              "source": "NOT_MODELLED",
              "value": null
            }
          ],
          "method": {
            "name": "FUTURE_VALUE_MONTHLY_COMPOUNDING",
            "remainingMonths": "n = calendar months from the month after asOfDate through the month of targetDate, both included; contributions arrive at the end of each month. A target date before asOfDate is EXPIRED; a target date later this month leaves n = 0 and is DUE_THIS_MONTH.",
            "monthlyRate": "r = expectedAnnualReturnPercent / 100 / 12, compounded monthly; g = (1 + r)^n.",
            "projectedAmount": "fromAllocated = currentAllocatedAmount × g; fromContributions = plannedMonthlyContribution × (g − 1) / r, or plannedMonthlyContribution × n when r = 0; total = fromAllocated + fromContributions; gapToTarget = total − targetAmount.",
            "requiredMonthlyContribution": "(targetAmount − currentAllocatedAmount × g) × r / (g − 1), or (targetAmount − currentAllocatedAmount) / n when r = 0; 0 when the goal is already funded or the allocated amount alone reaches the target; otherwise rounded up to the next 1,000 VND.",
            "status": "contributionCoveragePercent = plannedMonthlyContribution / requiredMonthlyContribution × 100 (100 when nothing is required). ON_TRACK at or above 100, AT_RISK at or above 80, BEHIND below; these are system reference thresholds, compared before the percentage is rounded.",
            "rounding": "Intermediate values carry 34 significant digits. Each part of the projected amount is rounded half up to 2 decimals before they are added, so the parts add up to the total. Percentages are rounded down to 2 decimals."
          },
          "disclaimer": "The expected annual return is the user's own assumption, not a guaranteed, promised or predicted return. Projected amounts illustrate what the entered figures would produce if that return were earned every month; actual results will differ."
        },
        "no planned monthly contribution": {
          "goalId": "4a7c2e91-5d08-4f3b-9c61-b2e8d0a5f714",
          "goalName": "Quỹ học tiếp cho bản thân",
          "currency": "VND",
          "asOfDate": "2026-11-22",
          "timeline": "ACTIVE",
          "targetReached": false,
          "completionPercent": 10,
          "remainingAmount": 360000000,
          "remainingMonths": 37,
          "daysPastTargetDate": null,
          "requiredMonthlyContribution": 9025000,
          "contributionCoveragePercent": null,
          "status": null,
          "projectedAtTargetDate": null,
          "calculation": {
            "monthlyRatePercent": 0.333333,
            "growthFactor": 1.1310294474
          },
          "unavailableReasons": [
            "PLANNED_MONTHLY_CONTRIBUTION_MISSING"
          ],
          "assumptions": [
            {
              "key": "TARGET_AMOUNT",
              "source": "USER_ENTERED",
              "value": "400000000.00"
            },
            {
              "key": "CURRENT_ALLOCATED_AMOUNT",
              "source": "USER_ENTERED",
              "value": "40000000.00"
            },
            {
              "key": "TARGET_DATE",
              "source": "USER_ENTERED",
              "value": "2029-12-31"
            },
            {
              "key": "PLANNED_MONTHLY_CONTRIBUTION",
              "source": "USER_ENTERED",
              "value": null
            },
            {
              "key": "EXPECTED_ANNUAL_RETURN_PERCENT",
              "source": "USER_ENTERED",
              "value": "4.00"
            },
            {
              "key": "AS_OF_DATE",
              "source": "SYSTEM",
              "value": "2026-11-22"
            },
            {
              "key": "TIME_ZONE",
              "source": "SYSTEM",
              "value": "Asia/Ho_Chi_Minh"
            },
            {
              "key": "FIRST_CONTRIBUTION_MONTH",
              "source": "SYSTEM",
              "value": "2026-12"
            },
            {
              "key": "COMPOUNDING",
              "source": "SYSTEM",
              "value": "MONTHLY"
            },
            {
              "key": "CONTRIBUTION_TIMING",
              "source": "SYSTEM",
              "value": "END_OF_MONTH"
            },
            {
              "key": "REQUIRED_CONTRIBUTION_ROUNDING",
              "source": "SYSTEM",
              "value": "UP_TO_1000_VND"
            },
            {
              "key": "ON_TRACK_MIN_COVERAGE_PERCENT",
              "source": "SYSTEM",
              "value": "100"
            },
            {
              "key": "AT_RISK_MIN_COVERAGE_PERCENT",
              "source": "SYSTEM",
              "value": "80"
            },
            {
              "key": "INFLATION",
              "source": "NOT_MODELLED",
              "value": null
            },
            {
              "key": "TAXES_AND_FEES",
              "source": "NOT_MODELLED",
              "value": null
            },
            {
              "key": "RETURN_VARIABILITY",
              "source": "NOT_MODELLED",
              "value": null
            },
            {
              "key": "CASH_FLOW_LINK",
              "source": "NOT_MODELLED",
              "value": null
            }
          ],
          "method": {
            "name": "FUTURE_VALUE_MONTHLY_COMPOUNDING",
            "remainingMonths": "n = calendar months from the month after asOfDate through the month of targetDate, both included; contributions arrive at the end of each month. A target date before asOfDate is EXPIRED; a target date later this month leaves n = 0 and is DUE_THIS_MONTH.",
            "monthlyRate": "r = expectedAnnualReturnPercent / 100 / 12, compounded monthly; g = (1 + r)^n.",
            "projectedAmount": "fromAllocated = currentAllocatedAmount × g; fromContributions = plannedMonthlyContribution × (g − 1) / r, or plannedMonthlyContribution × n when r = 0; total = fromAllocated + fromContributions; gapToTarget = total − targetAmount.",
            "requiredMonthlyContribution": "(targetAmount − currentAllocatedAmount × g) × r / (g − 1), or (targetAmount − currentAllocatedAmount) / n when r = 0; 0 when the goal is already funded or the allocated amount alone reaches the target; otherwise rounded up to the next 1,000 VND.",
            "status": "contributionCoveragePercent = plannedMonthlyContribution / requiredMonthlyContribution × 100 (100 when nothing is required). ON_TRACK at or above 100, AT_RISK at or above 80, BEHIND below; these are system reference thresholds, compared before the percentage is rounded.",
            "rounding": "Intermediate values carry 34 significant digits. Each part of the projected amount is rounded half up to 2 decimals before they are added, so the parts add up to the total. Percentages are rounded down to 2 decimals."
          },
          "disclaimer": "The expected annual return is the user's own assumption, not a guaranteed, promised or predicted return. Projected amounts illustrate what the entered figures would produce if that return were earned every month; actual results will differ."
        }
      }
    },
    "404": {
      "contentType": "application/problem+json",
      "examples": {
        "absent or another user's": {
          "type": "urn:rikkaus:problem:not-found",
          "title": "Resource not found",
          "status": 404,
          "detail": "Resource not found.",
          "instance": "/api/v1/goal-projections/9d4b1f68-2c7e-4a35-b8f0-6e1a5d3c7b92",
          "correlationId": "b7f4c21a-9e03-4d55-8a16-2cf09b4d7e83"
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
  },
  "GET /api/v1/assets/{assetId}/deletion-impact": {
    "200": {
      "contentType": "application/json",
      "examples": {
        "Apartment pledged for a mortgage and rented out": {
          "recordType": "ASSET",
          "recordId": "3f6b2a10-8c4d-4e5f-9a1b-2c3d4e5f6a7b",
          "confirmationRequired": true,
          "linkedEffects": [
            {
              "recordType": "ASSET_VALUATION",
              "effect": "DELETED",
              "field": null,
              "count": 3,
              "records": []
            },
            {
              "recordType": "LIABILITY",
              "effect": "UNLINKED",
              "field": "collateralAssetId",
              "count": 1,
              "records": [
                {
                  "id": "9d1e7c42-5b6a-4f3e-8d2c-1b0a9f8e7d6c",
                  "label": "Vay mua nhà Vietcombank",
                  "category": "BANK_LOAN"
                }
              ]
            },
            {
              "recordType": "CASH_FLOW_ENTRY",
              "effect": "UNLINKED",
              "field": "linkedAssetId",
              "count": 1,
              "records": [
                {
                  "id": "c2a4e6f8-1b3d-4f5a-8c7e-9d0b2a4c6e8f",
                  "label": "Cho thuê căn hộ Thảo Điền",
                  "category": "RENTAL_INCOME"
                }
              ]
            }
          ],
          "derivedEffects": []
        }
      }
    }
  }
} as const;
