// Copy-paste template only — not imported anywhere. The live version is components/stat-tile.js.
export function StatTile({ label, value }) {
  return (
    <div className="stat-tile">
      <div className="stat-tile-label">{label}</div>
      <div className="stat-tile-value">{value}</div>
    </div>
  );
}

export function StatTileRow({ children }) {
  return <div className="stat-tile-row">{children}</div>;
}
