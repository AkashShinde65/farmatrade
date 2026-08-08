import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { MapContainer, TileLayer, Marker, useMap, useMapEvents } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import markerIcon2x from 'leaflet/dist/images/marker-icon-2x.png';
import markerIcon from 'leaflet/dist/images/marker-icon.png';
import markerShadow from 'leaflet/dist/images/marker-shadow.png';
import { useAuth } from '../hooks/auth-hook';
import * as lotService from '../services/lot-service';
import { forwardGeocode, reverseGeocode } from '../utils/geocoding';

delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: markerIcon2x,
  iconUrl: markerIcon,
  shadowUrl: markerShadow,
});

// lot-service's CreateLotRequest requires this field for an AUCTION lot even on update, but
// LotService.updateLot() deliberately leaves an existing auction's real end time untouched (see
// its "Do NOT reset auctionEndsAt" comment) -- so this value only matters for validation here, not
// for actually changing when a live auction ends.
const DEFAULT_AUCTION_DURATION_MINUTES = 60;

function LocationPicker({ position, onPick }) {
  useMapEvents({
    click(event) {
      onPick(event.latlng);
    },
  });
  return position ? <Marker position={position} /> : null;
}

// MapContainer's own `center` prop only applies once, on first mount -- this re-centers the view
// whenever a location is found by typing a place name.
function MapRecenter({ position }) {
  const map = useMap();
  useEffect(() => {
    if (position) {
      map.setView([position.lat, position.lng], 12);
    }
  }, [position, map]);
  return null;
}

export function LotEditPage() {
  const { id } = useParams();
  const { user, token } = useAuth();
  const navigate = useNavigate();

  const [loading, setLoading] = useState(true);
  const [cropName, setCropName] = useState('');
  const [grade, setGrade] = useState('A');
  const [quantity, setQuantity] = useState('');
  const [unit, setUnit] = useState('KG');
  const [saleType, setSaleType] = useState('AUCTION');
  const [basePrice, setBasePrice] = useState('');
  const [fixedPrice, setFixedPrice] = useState('');
  const [auctionDurationMinutes, setAuctionDurationMinutes] = useState(String(DEFAULT_AUCTION_DURATION_MINUTES));
  const [locationName, setLocationName] = useState('');
  const [position, setPosition] = useState(null);
  const [image, setImage] = useState(null);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [locatingName, setLocatingName] = useState(false);
  const [findingOnMap, setFindingOnMap] = useState(false);

  useEffect(() => {
    let cancelled = false;
    lotService
      .getLotById(id)
      .then((lot) => {
        if (cancelled) return;
        setCropName(lot.cropName);
        setGrade(lot.grade);
        setQuantity(String(lot.quantity));
        setUnit(lot.unit);
        setSaleType(lot.saleType);
        setBasePrice(lot.basePrice != null ? String(lot.basePrice) : '');
        setFixedPrice(lot.fixedPrice != null ? String(lot.fixedPrice) : '');
        setLocationName(lot.locationName ?? '');
        if (lot.latitude != null && lot.longitude != null) {
          setPosition({ lat: lot.latitude, lng: lot.longitude });
        }
      })
      .catch((err) => setError(err.message || 'Could not load this lot.'))
      .finally(() => setLoading(false));
    return () => {
      cancelled = true;
    };
  }, [id]);

  async function handleMapPick(latlng) {
    setPosition(latlng);
    setLocatingName(true);
    const name = await reverseGeocode(latlng.lat, latlng.lng);
    setLocatingName(false);
    if (name) {
      setLocationName(name);
    }
  }

  // The other direction of handleMapPick: typing a place name moves the marker instead of
  // clicking it into place.
  async function handleFindOnMap() {
    if (!locationName.trim()) {
      return;
    }
    setError('');
    setFindingOnMap(true);
    const coords = await forwardGeocode(locationName.trim());
    setFindingOnMap(false);
    if (coords) {
      setPosition(coords);
    } else {
      setError("Could not find that place on the map. Try a different spelling, or click the map directly.");
    }
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');

    if (!position) {
      setError("Click the map to set the lot's location.");
      return;
    }

    setSubmitting(true);
    try {
      await lotService.updateLot(
        id,
        {
          farmerId: user.id,
          cropName,
          grade,
          quantity,
          unit,
          saleType,
          basePrice: saleType === 'AUCTION' ? basePrice : undefined,
          fixedPrice: saleType === 'FIXED_PRICE' ? fixedPrice : undefined,
          auctionDurationMinutes: saleType === 'AUCTION' ? auctionDurationMinutes : undefined,
          latitude: position.lat,
          longitude: position.lng,
          locationName,
        },
        image,
        token
      );

      navigate('/farmer/dashboard');
    } catch (err) {
      setError(err.message || 'Could not update the lot.');
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) {
    return (
      <div className="page">
        <p className="loading-text">Loading…</p>
      </div>
    );
  }

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Update lot</h1>
          <p className="page-subtitle">Change the price or other details of this listing.</p>
        </div>
      </div>

      <div className="card">
        <div className="card-body">
          <form className="form" style={{ maxWidth: 'none' }} onSubmit={handleSubmit}>
            <div className="field">
              <label className="field-label" htmlFor="cropName">Crop</label>
              <input id="cropName" className="input" value={cropName} onChange={(e) => setCropName(e.target.value)} required />
            </div>

            <div className="field">
              <label className="field-label" htmlFor="grade">Grade</label>
              <select id="grade" className="select" value={grade} onChange={(e) => setGrade(e.target.value)}>
                <option value="A">A — Premium</option>
                <option value="B">B — Standard</option>
                <option value="C">C — Below standard</option>
              </select>
            </div>

            <div className="field">
              <label className="field-label" htmlFor="quantity">Quantity</label>
              <input
                id="quantity"
                className="input"
                type="number"
                min="0"
                step="0.01"
                value={quantity}
                onChange={(e) => setQuantity(e.target.value)}
                required
              />
            </div>

            <div className="field">
              <label className="field-label" htmlFor="unit">Unit</label>
              <select id="unit" className="select" value={unit} onChange={(e) => setUnit(e.target.value)}>
                <option value="KG">KG</option>
                <option value="TON">TON</option>
                <option value="QUINTAL">QUINTAL</option>
              </select>
            </div>

            <div className="field">
              <span className="field-label">Sale mode</span>
              <div className="role-tabs">
                <button type="button" className={saleType === 'AUCTION' ? 'active' : ''} onClick={() => setSaleType('AUCTION')}>
                  Auction
                </button>
                <button type="button" className={saleType === 'FIXED_PRICE' ? 'active' : ''} onClick={() => setSaleType('FIXED_PRICE')}>
                  Fixed price
                </button>
              </div>
            </div>

            {saleType === 'AUCTION' ? (
              <>
                <div className="field">
                  <label className="field-label" htmlFor="basePrice">Base price (₹)</label>
                  <input
                    id="basePrice"
                    className="input"
                    type="number"
                    min="0.01"
                    step="0.01"
                    value={basePrice}
                    onChange={(e) => setBasePrice(e.target.value)}
                    required
                  />
                  <span className="field-hint">Minimum bid the auction starts from.</span>
                </div>
                <div className="field">
                  <label className="field-label" htmlFor="auctionDurationMinutes">Auction duration (minutes)</label>
                  <input
                    id="auctionDurationMinutes"
                    className="input"
                    type="number"
                    min="1"
                    value={auctionDurationMinutes}
                    onChange={(e) => setAuctionDurationMinutes(e.target.value)}
                    required
                  />
                  <span className="field-hint">Has no effect on an auction that's already running.</span>
                </div>
              </>
            ) : (
              <div className="field">
                <label className="field-label" htmlFor="fixedPrice">Fixed price (₹)</label>
                <input
                  id="fixedPrice"
                  className="input"
                  type="number"
                  min="0.01"
                  step="0.01"
                  value={fixedPrice}
                  onChange={(e) => setFixedPrice(e.target.value)}
                  required
                />
                <span className="field-hint">First buyer who pays this price wins instantly.</span>
              </div>
            )}

            <div className="field">
              <label className="field-label" htmlFor="locationName">Lot location</label>
              <div style={{ display: 'flex', gap: 8 }}>
                <input
                  id="locationName"
                  className="input"
                  value={locationName}
                  onChange={(e) => setLocationName(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') {
                      e.preventDefault();
                      handleFindOnMap();
                    }
                  }}
                  required
                  style={{ flex: 1 }}
                />
                <button type="button" className="btn btn-secondary" onClick={handleFindOnMap} disabled={findingOnMap}>
                  {findingOnMap ? 'Finding…' : 'Find on map'}
                </button>
              </div>
              <span className="field-hint">Type a city/state and click "Find on map", or click the map directly to change the exact pickup point.</span>
              <div style={{ height: 260, marginTop: 8, borderRadius: 'var(--radius)', overflow: 'hidden' }}>
                <MapContainer
                  center={position ?? [20.5937, 78.9629]}
                  zoom={position ? 10 : 5}
                  style={{ height: '100%', width: '100%' }}
                >
                  <TileLayer attribution="&copy; OpenStreetMap contributors" url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
                  <LocationPicker position={position} onPick={handleMapPick} />
                  <MapRecenter position={position} />
                </MapContainer>
              </div>
              {position ? (
                <span className="field-hint">
                  {locatingName ? 'Looking up location name…' : null} Lat: {position.lat.toFixed(4)}, Lng: {position.lng.toFixed(4)}
                </span>
              ) : null}
            </div>

            <div className="field">
              <label className="field-label" htmlFor="image">Replace photo (optional)</label>
              <input id="image" type="file" accept="image/*" onChange={(e) => setImage(e.target.files?.[0] || null)} />
            </div>

            {error ? <p className="form-error">{error}</p> : null}

            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? 'Saving…' : 'Save changes'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
