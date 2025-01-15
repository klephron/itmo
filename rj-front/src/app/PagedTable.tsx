import { createSignal, createMemo } from 'solid-js';

import styles from './PagedTable.module.scss';

type Props<T> = {
  data: T[];
  headers: string[];
  rowsPerPage: number;
  fields: ((e: T) => string)[];
};

export default function PagedTable<T>(props: Props<T>) {
  const { data, headers, rowsPerPage = 10, fields } = props;

  const [currentPage, setCurrentPage] = createSignal(0);
  const totalPages = createMemo(() => Math.ceil(data.length / rowsPerPage));
  const currentData = createMemo(() =>
    data.slice(currentPage() * rowsPerPage, (currentPage() + 1) * rowsPerPage)
  );

  const goToPage = (page: number) => {
    if (page >= 0 && page < totalPages()) {
      setCurrentPage(page);
    }
  };

  return (
    <div class={styles.paged_table}>
      <table class={styles.paged_table_header}>
        <thead>
          <tr>
            {headers.map((key) => (
              <th>{key}</th>
            ))}
          </tr>
        </thead>
        <tbody class={styles.paged_table_body}>
          {currentData().map((element, _) => (
            <tr>
              {fields.map((mapper) => (
                <td>{mapper(element)}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
      <div class={styles.paged_table_control}>
        <button onClick={() => goToPage(currentPage() - 1)} disabled={currentPage() === 0}>
          Previous
        </button>
        <span>
          Page {currentPage() + 1} of {totalPages()}
        </span>
        <button
          onClick={() => goToPage(currentPage() + 1)}
          disabled={currentPage() + 1 === totalPages()}
        >
          Next
        </button>
      </div>
    </div>
  );
}
