import { useClient } from "@state/client";
import { hasPermission, usePermissions } from "@state/permissions";
import { useEffect, useMemo, useState } from "react";
import { LogEntryDto, PageLogEntryDto } from "@api/gen/data-contracts";
import styles from "./index.module.scss";
import { datetimeLocalToUtcIso, formatUtc } from "@util";

type Filters = {
  level: string;
  logger: string;
  message: string;
  from: string;
  to: string;
  page: number;
  size: number;
};

function formatDate(value?: string | null) {
  return formatUtc(value);
}

function toInputValue(value?: string) {
  if (!value) return "";
  try {
    return new Date(value).toISOString().slice(0, 16);
  } catch {
    return "";
  }
}

function toIso(value?: string) {
  return datetimeLocalToUtcIso(value);
}

export default function LogsPage() {
  const client = useClient((state) => state.client);
  const permissions = usePermissions((state) => state.permissions);
  const canRead = useMemo(() => !!permissions && hasPermission("LOGS:*:READ"), [permissions]);

  const [filters, filtersSet] = useState<Filters>({
    level: "",
    logger: "",
    message: "",
    from: "",
    to: "",
    page: 0,
    size: 50,
  });
  const [pageData, pageDataSet] = useState<PageLogEntryDto | null>(null);
  const [loading, loadingSet] = useState(false);
  const [error, errorSet] = useState<string | null>(null);

  const load = () => {
    if (!canRead) return;
    loadingSet(true);
    errorSet(null);
    client.logs
      .getLogs({
        page: filters.page,
        size: filters.size,
        level: filters.level || undefined,
        logger: filters.logger || undefined,
        message: filters.message || undefined,
        from: toIso(filters.from),
        to: toIso(filters.to),
      })
      .then((r) => pageDataSet(r.data))
      .catch((e) => {
        console.error(e);
        const msg = typeof e?.error === "string" ? e.error : e?.message || "Failed to load logs";
        errorSet(msg);
      })
      .finally(() => loadingSet(false));
  };

  useEffect(() => {
    load();
  }, [filters.page, filters.size, canRead]);

  if (!canRead) {
    return (
      <div className={styles.root}>
        <h2>Logs</h2>
        <span className={styles.hint}>Нет прав (LOGS:*:READ)</span>
      </div>
    );
  }

  return (
    <div className={styles.root}>
      <h2>Logs</h2>
      <div className={styles.filters}>
        <label>
          Level
          <select
            value={filters.level}
            onChange={(e) => filtersSet({ ...filters, level: e.target.value, page: 0 })}
          >
            <option value="">Any</option>
            <option value="TRACE">TRACE</option>
            <option value="DEBUG">DEBUG</option>
            <option value="INFO">INFO</option>
            <option value="WARN">WARN</option>
            <option value="ERROR">ERROR</option>
            <option value="FATAL">FATAL</option>
          </select>
        </label>
        <label>
          Logger contains
          <input
            value={filters.logger}
            onChange={(e) => filtersSet({ ...filters, logger: e.target.value, page: 0 })}
          />
        </label>
        <label>
          Message contains
          <input
            value={filters.message}
            onChange={(e) => filtersSet({ ...filters, message: e.target.value, page: 0 })}
          />
        </label>
        <label>
          From
          <input
            type="datetime-local"
            value={filters.from}
            onChange={(e) => filtersSet({ ...filters, from: e.target.value, page: 0 })}
            placeholder="2025-01-01T00:00:00Z"
          />
        </label>
        <label>
          To
          <input
            type="datetime-local"
            value={filters.to}
            onChange={(e) => filtersSet({ ...filters, to: e.target.value, page: 0 })}
            placeholder="2025-01-31T23:59:59Z"
          />
        </label>
        <label>
          Size
          <input
            type="number"
            min={1}
            value={filters.size}
            onChange={(e) =>
              filtersSet({ ...filters, size: Number(e.target.value) || 50, page: 0 })
            }
          />
        </label>
        <button type="button" onClick={load} disabled={loading}>
          Search
        </button>
        {error ? <span className={styles.hint}>{error}</span> : null}
      </div>

      {loading && <span>Loading...</span>}

      {pageData ? (
        <>
          <table className={styles.table}>
            <thead>
              <tr className={styles.row}>
                <th>Time (UTC)</th>
                <th>Level</th>
                <th>Logger</th>
                <th>Thread</th>
                <th>Message</th>
                <th>Exception</th>
              </tr>
            </thead>
            <tbody>
              {pageData.content.map((log: LogEntryDto) => (
                <tr className={styles.row} key={log.id ?? `${log.createdAt}-${log.loggerName}`}>
                  <td>{formatDate(log.createdAt)}</td>
                  <td>{log.level}</td>
                  <td>{log.loggerName}</td>
                  <td>{log.threadName}</td>
                  <td>{log.message}</td>
                  <td>{log.exception ?? ""}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className={styles.pagination}>
            <button
              type="button"
              onClick={() => filtersSet({ ...filters, page: Math.max(0, filters.page - 1) })}
              disabled={filters.page <= 0 || loading}
            >
              Prev
            </button>
            <span>
              Page {filters.page + 1}
              {pageData.totalPages ? ` / ${pageData.totalPages}` : ""}
            </span>
            <button
              type="button"
              onClick={() => filtersSet({ ...filters, page: filters.page + 1 })}
              disabled={
                loading ||
                (pageData.totalPages !== undefined && pageData.totalPages !== null
                  ? filters.page + 1 >= pageData.totalPages
                  : false)
              }
            >
              Next
            </button>
            {pageData.totalSize !== undefined ? (
              <span>Total: {pageData.totalSize}</span>
            ) : null}
          </div>
        </>
      ) : null}
    </div>
  );
}
