// Both directions use Nominatim, the same free OpenStreetMap geocoding service
// logistics-service already uses on the backend for forward geocoding.

// Reverse-geocodes a map click into a short "City, State" style name. Nominatim's own reverse
// endpoint returns a very long, full-postal-address display_name; this instead builds a short
// name from the structured address fields, matching the "e.g. Nashik, MH" style the location
// field asks for.
export async function reverseGeocode(lat, lng) {
  try {
    const response = await fetch(
      `https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=${lat}&lon=${lng}&zoom=10`,
      { headers: { Accept: 'application/json' } }
    );
    if (!response.ok) {
      return null;
    }
    const data = await response.json();
    const address = data.address || {};
    const place = address.city || address.town || address.village || address.county || address.state_district;
    const state = address.state;
    if (place && state) {
      return `${place}, ${state}`;
    }
    return place || state || data.display_name || null;
  } catch {
    return null;
  }
}

// Forward-geocodes a typed place name (e.g. "Nashik, MH") into coordinates, the reverse of
// reverseGeocode above -- lets a farmer type a city/state instead of having to find it on the map.
export async function forwardGeocode(query) {
  try {
    const response = await fetch(
      `https://nominatim.openstreetmap.org/search?format=jsonv2&q=${encodeURIComponent(query)}&limit=1`,
      { headers: { Accept: 'application/json' } }
    );
    if (!response.ok) {
      return null;
    }
    const results = await response.json();
    if (!results.length) {
      return null;
    }
    return { lat: Number(results[0].lat), lng: Number(results[0].lon) };
  } catch {
    return null;
  }
}
