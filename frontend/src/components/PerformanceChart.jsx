function chartPoints(metrics, key, width, height, padding) {
  const values = metrics.map((metric) => Number(metric[key])).filter(Number.isFinite);
  if (!values.length) return { points: "", values: [] };
  const min = Math.min(...values);
  const max = Math.max(...values);
  const range = Math.max(max - min, 1);
  const points = values
    .map((value, index) => {
      const x = padding + (index * (width - padding * 2)) / Math.max(values.length - 1, 1);
      const y = height - padding - ((value - min) / range) * (height - padding * 2);
      return `${x},${y}`;
    })
    .join(" ");
  return { points, values };
}

export default function PerformanceChart({ metrics }) {
  const width = 680;
  const height = 240;
  const padding = 28;
  const memory = chartPoints(metrics, "memory_score", width, height, padding);
  const attention = chartPoints(metrics, "attention_score", width, height, padding);
  const allValues = [...memory.values, ...attention.values];
  const min = allValues.length ? Math.floor(Math.min(...allValues) - 3) : 0;
  const max = allValues.length ? Math.ceil(Math.max(...allValues) + 3) : 100;
  const labels = metrics.filter((_, index) => index === 0 || index === metrics.length - 1);

  if (!metrics.length) {
    return <div className="chart-empty">Performance history will appear here.</div>;
  }

  return (
    <div className="chart-wrap">
      <div className="chart-legend">
        <span><i className="legend-dot memory" />Memory</span>
        <span><i className="legend-dot attention" />Attention</span>
        <span className="chart-scale">{min}–{max} game performance points</span>
      </div>
      <svg className="performance-chart" viewBox={`0 0 ${width} ${height}`} role="img" aria-label="Memory and attention performance trend">
        {[0, 1, 2, 3].map((line) => {
          const y = padding + (line * (height - padding * 2)) / 3;
          return <line key={line} x1={padding} x2={width - padding} y1={y} y2={y} className="chart-grid" />;
        })}
        <polyline points={memory.points} className="chart-line memory-line" />
        <polyline points={attention.points} className="chart-line attention-line" />
      </svg>
      <div className="chart-labels">
        <span>{formatShortDate(labels[0]?.metric_date)}</span>
        <span>{formatShortDate(labels[labels.length - 1]?.metric_date)}</span>
      </div>
    </div>
  );
}

export function formatShortDate(value) {
  if (!value) return "";
  return new Intl.DateTimeFormat("en-IN", { day: "numeric", month: "short" }).format(new Date(`${value}T00:00:00`));
}
