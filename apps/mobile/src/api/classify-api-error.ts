import { isApiError } from './api-error';
import type { KnownProblemType } from './contract-types';
import { isKnownProblemType } from './problem';

/**
 * What a screen needs to render a failure. Copy is chosen from the problem `type` URN (or the
 * failure kind), never from the server's `detail` or `errors[].message`, which are English and
 * written for developers.
 */
export interface ApiErrorPresentation {
  kind: 'configuration' | 'network' | 'problem' | 'http' | 'schema-mismatch' | 'unknown';
  problemType: string | null;
  status: number | null;
  /** Short heading, e.g. `Phiên đăng nhập đã hết hạn`. */
  title: string;
  /** What happened and what the user can do next. */
  message: string;
  /** Matches `ErrorState`'s variant: offline failures are not worth retrying immediately. */
  variant: 'request' | 'offline';
  retryable: boolean;
  /** True only for `unauthorized`; the identity layer decides how to sign in again. */
  requiresSignIn: boolean;
  /** Rejected field names from a validation failure, for forms to mark inline. */
  fields: string[];
  correlationId: string | null;
  /** `Mã tham chiếu: …`, for support, or null when the server sent no identifier. */
  referenceLabel: string | null;
}

interface Copy {
  title: string;
  message: string;
  retryable: boolean;
}

const CLIENT_DEFECT: Copy = {
  title: 'Ứng dụng cần được cập nhật',
  message:
    'Phiên bản ứng dụng này gửi yêu cầu mà máy chủ không nhận. Hãy tải lại hoặc cập nhật ứng dụng.',
  retryable: false,
};

const UNAVAILABLE_TARGET: Copy = {
  title: 'Không thể mở mục này',
  message: 'Quay lại trang trước để tiếp tục.',
  retryable: false,
};

/**
 * One entry per contract URN; the `Record<KnownProblemType, …>` type fails compilation when the
 * contract adds a URN this map does not cover. `not-found` and `forbidden` share the
 * non-enumerating copy required by design-guidelines §11.1, so neither confirms the record exists.
 * The `unauthorized` copy is the accepted identity design's expired-session text.
 */
const PROBLEM_COPY: Record<KnownProblemType, Copy> = {
  'urn:rikkaus:problem:validation-failed': {
    title: 'Thông tin chưa hợp lệ',
    message: 'Một số thông tin chưa hợp lệ. Kiểm tra các trường được đánh dấu rồi gửi lại.',
    retryable: false,
  },
  'urn:rikkaus:problem:malformed-request': {
    title: 'Yêu cầu không đọc được',
    message: 'Máy chủ không đọc được dữ liệu vừa gửi. Tải lại trang rồi thử lại.',
    retryable: false,
  },
  'urn:rikkaus:problem:not-found': UNAVAILABLE_TARGET,
  'urn:rikkaus:problem:method-not-allowed': CLIENT_DEFECT,
  'urn:rikkaus:problem:not-acceptable': CLIENT_DEFECT,
  'urn:rikkaus:problem:unsupported-media-type': CLIENT_DEFECT,
  'urn:rikkaus:problem:unauthorized': {
    title: 'Phiên đăng nhập đã hết hạn',
    message:
      'Để bảo vệ dữ liệu tài chính, nội dung trước đó đã được ẩn. Tiếp tục với Google để đăng nhập lại.',
    retryable: false,
  },
  'urn:rikkaus:problem:forbidden': UNAVAILABLE_TARGET,
  'urn:rikkaus:problem:internal-error': {
    title: 'Máy chủ gặp sự cố',
    message: 'Rikkaus chưa xử lý được yêu cầu này. Thử lại sau ít phút.',
    retryable: true,
  },
};

const UNKNOWN_PROBLEM: Copy = {
  title: 'Yêu cầu chưa hoàn tất',
  message: 'Máy chủ chưa hoàn tất yêu cầu này. Thử lại sau ít phút.',
  retryable: false,
};

const SERVER_UNAVAILABLE: Copy = {
  title: 'Máy chủ chưa sẵn sàng',
  message: 'Máy chủ đang tạm thời không phục vụ. Thử lại sau ít phút.',
  retryable: true,
};

const NETWORK: Copy = {
  title: 'Không kết nối được máy chủ',
  message: 'Kiểm tra kết nối mạng rồi thử lại.',
  retryable: true,
};

const SCHEMA_MISMATCH: Copy = {
  title: 'Dữ liệu trả về không đúng định dạng',
  message: 'Ứng dụng có thể chưa tương thích với máy chủ. Hãy tải lại hoặc cập nhật ứng dụng.',
  retryable: false,
};

const CONFIGURATION: Copy = {
  title: 'Chưa cấu hình đúng địa chỉ API',
  message: 'Đặt EXPO_PUBLIC_API_BASE_URL thành một địa chỉ http hoặc https rồi khởi động lại Expo.',
  retryable: false,
};

const UNEXPECTED: Copy = {
  title: 'Đã có lỗi ngoài dự kiến',
  message: 'Thao tác chưa hoàn tất. Thử lại sau ít phút.',
  retryable: false,
};

function copyForStatus(status: number): Copy {
  return status >= 500 ? SERVER_UNAVAILABLE : UNKNOWN_PROBLEM;
}

export function formatReferenceLabel(correlationId: string | null): string | null {
  return correlationId ? `Mã tham chiếu: ${correlationId}` : null;
}

export function classifyApiError(error: unknown): ApiErrorPresentation {
  if (!isApiError(error)) {
    return {
      ...UNEXPECTED,
      kind: 'unknown',
      problemType: null,
      status: null,
      variant: 'request',
      requiresSignIn: false,
      fields: [],
      correlationId: null,
      referenceLabel: null,
    };
  }

  const { failure, correlationId } = error;
  const base = {
    correlationId,
    referenceLabel: formatReferenceLabel(correlationId),
    fields: [] as string[],
    requiresSignIn: false,
    variant: 'request' as const,
  };

  switch (failure.kind) {
    case 'configuration':
      return { ...base, ...CONFIGURATION, kind: 'configuration', problemType: null, status: null };
    case 'network':
      return {
        ...base,
        ...NETWORK,
        kind: 'network',
        problemType: null,
        status: null,
        variant: 'offline',
      };
    case 'http':
      return {
        ...base,
        ...copyForStatus(failure.status),
        kind: 'http',
        problemType: null,
        status: failure.status,
      };
    case 'schema-mismatch':
      return {
        ...base,
        ...SCHEMA_MISMATCH,
        kind: 'schema-mismatch',
        problemType: null,
        status: failure.status,
      };
    case 'problem': {
      const { problem, status } = failure;
      const copy = isKnownProblemType(problem.type)
        ? PROBLEM_COPY[problem.type]
        : copyForStatus(status);

      return {
        ...base,
        ...copy,
        kind: 'problem',
        problemType: problem.type,
        status,
        requiresSignIn: problem.type === 'urn:rikkaus:problem:unauthorized',
        fields: (problem.errors ?? [])
          .map((entry) => entry.field)
          .filter((field): field is string => Boolean(field)),
      };
    }
  }
}
