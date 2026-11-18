/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
import type { Page } from '../api/types';

interface Props {
  page: Page<unknown>['page'];
  onChange: (pageNumber: number) => void;
}

export default function Pager({ page, onChange }: Props) {
  const totalPages = Math.max(page.totalPages, 1);
  return (
    <div className="pager">
      <button disabled={page.number <= 0} onClick={() => onChange(page.number - 1)}>
        Prev
      </button>
      <span>
        Page {page.number + 1} of {totalPages} ({page.totalElements} total)
      </span>
      <button disabled={page.number + 1 >= totalPages} onClick={() => onChange(page.number + 1)}>
        Next
      </button>
    </div>
  );
}
