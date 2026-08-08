export class ApiError extends Error {
  constructor(message, status, body) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.body = body;
  }
}

async function request(baseUrl, path, { method = 'GET', body, token, isFormData = false, responseType = 'json' } = {}) {
  const headers = {};
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  let finalBody = body;
  if (body !== undefined && !isFormData) {
    headers['Content-Type'] = 'application/json';
    finalBody = JSON.stringify(body);
  }

  const response = await fetch(`${baseUrl}${path}`, {
    method,
    headers,
    body: finalBody,
  });

  if (!response.ok) {
    let errorBody = null;
    try {
      errorBody = await response.json();
    } catch {
      // no JSON error body
    }
    const message = errorBody?.message || errorBody?.error || `Request failed with status ${response.status}`;
    throw new ApiError(message, response.status, errorBody);
  }

  if (response.status === 204) {
    return null;
  }

  if (responseType === 'blob') {
    return response.blob();
  }

  const text = await response.text();
  return text ? JSON.parse(text) : null;
}

export function createApiClient(baseUrl) {
  return {
    get: (path, token) => request(baseUrl, path, { method: 'GET', token }),
    post: (path, body, token) => request(baseUrl, path, { method: 'POST', body, token }),
    postForm: (path, formData, token) => request(baseUrl, path, { method: 'POST', body: formData, token, isFormData: true }),
    put: (path, body, token) => request(baseUrl, path, { method: 'PUT', body, token }),
    putForm: (path, formData, token) => request(baseUrl, path, { method: 'PUT', body: formData, token, isFormData: true }),
    patch: (path, body, token) => request(baseUrl, path, { method: 'PATCH', body, token }),
    delete: (path, token) => request(baseUrl, path, { method: 'DELETE', token }),
    getBlob: (path, token) => request(baseUrl, path, { method: 'GET', token, responseType: 'blob' }),
  };
}
