import { createApiClient } from './http';

const client = createApiClient(process.env.REACT_APP_LOT_URL || 'http://localhost:8082');

// CreateLotRequest fields: farmerId, cropName, grade, quantity, unit, saleType,
// basePrice, fixedPrice, auctionDurationMinutes, latitude, longitude, locationName.
// lot-service expects this as multipart/form-data (not JSON), with an optional "image" part.
function buildLotFormData(fields, image) {
  const formData = new FormData();
  Object.entries(fields).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      formData.append(key, value);
    }
  });
  if (image) {
    formData.append('image', image);
  }
  return formData;
}

export function createLot(fields, image, token) {
  return client.postForm('/api/lots', buildLotFormData(fields, image), token);
}

export function getAllLots() {
  return client.get('/api/lots');
}

export function getLotById(id) {
  return client.get(`/api/lots/${id}`);
}

export function getLotsByFarmer(farmerId, token) {
  return client.get(`/api/lots/farmer/${farmerId}`, token);
}

export function searchLotsByCrop(cropName) {
  return client.get(`/api/lots/search?cropName=${encodeURIComponent(cropName)}`);
}

export function cancelLot(id, token) {
  return client.delete(`/api/lots/${id}`, token);
}

// Unlike createLot, LotController's PUT /api/lots/{id} expects the CreateLotRequest fields as a
// single JSON string under a "lot" part, not as individual form fields -- a different contract
// from creation, not a typo.
export function updateLot(id, fields, image, token) {
  const formData = new FormData();
  formData.append('lot', JSON.stringify(fields));
  if (image) {
    formData.append('image', image);
  }
  return client.putForm(`/api/lots/${id}`, formData, token);
}
