// columns: [{ key, label, render?(row) }]
export function DataTable({ columns, rows, rowKey = 'id', emptyText = 'Nothing to show yet.' }) {
  if (!rows || rows.length === 0) {
    return <p className="empty-text">{emptyText}</p>;
  }

  return (
    <table className="data-table">
      <thead>
        <tr>
          {columns.map((column) => (
            <th key={column.key}>{column.label}</th>
          ))}
        </tr>
      </thead>
      <tbody>
        {rows.map((row) => (
          <tr key={row[rowKey]}>
            {columns.map((column) => (
              <td key={column.key}>{column.render ? column.render(row) : row[column.key]}</td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  );
}
