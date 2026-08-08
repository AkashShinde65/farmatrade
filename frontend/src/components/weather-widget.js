import { StatusBadge } from './status-badge';

const RISK_TONE = { LOW: 'success', MEDIUM: 'warning', HIGH: 'danger' };

function dayLabel(dateValue) {
  if (!dateValue) return '—';
  const date = new Date(dateValue);
  if (Number.isNaN(date.getTime())) return '—';
  return date.toLocaleDateString('en-IN', { weekday: 'short' });
}

export function WeatherWidget({ weather }) {
  if (!weather) return null;

  return (
    <div className="weather-widget">
      <div className="weather-widget-current">
        <StatusBadge status={`${weather.riskLevel} RISK`} tone={RISK_TONE[weather.riskLevel] || 'neutral'} />
        <span>{weather.currentTempC}°C</span>
        <span>{weather.currentHumidityPct}% humidity</span>
      </div>
      {weather.forecast && weather.forecast.length > 0 ? (
        <div className="weather-forecast">
          {weather.forecast.map((day) => (
            <div key={day.date} className="weather-forecast-day">
              <div>{dayLabel(day.date)}</div>
              <div>
                {day.maxTempC}° / {day.minTempC}°
              </div>
              <div>{day.avgHumidityPct}%</div>
              <div>{day.precipitationMm}mm</div>
            </div>
          ))}
        </div>
      ) : null}
    </div>
  );
}
