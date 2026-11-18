/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { json, mockFetch } from '../../test/mockFetch';
import RollCallTab from './RollCallTab';

const STUDENTS = [
  { id: 1, fullName: 'Alice Smith', studentNo: 'S001' },
  { id: 2, fullName: 'Bob Jones', studentNo: 'S002' },
];

describe('RollCallTab', () => {
  it('renders enrolled students and PUTs the selected statuses', async () => {
    const calls = mockFetch(({ method, url }) => {
      if (url === '/api/classes/7/students') return json(200, STUDENTS);
      if (method === 'GET' && url.startsWith('/api/classes/7/attendance?date=')) {
        return json(200, [{ id: 99, classId: 7, studentId: 2, status: 'LATE' }]);
      }
      if (method === 'PUT' && url === '/api/classes/7/attendance') return json(200, []);
    });
    render(<RollCallTab classId={7} canManage />);

    expect(await screen.findByText('Alice Smith')).toBeInTheDocument();
    expect(screen.getByText('Bob Jones')).toBeInTheDocument();
    // The existing record is applied; others default to PRESENT.
    expect(screen.getByLabelText('Status for Alice Smith')).toHaveValue('PRESENT');
    expect(screen.getByLabelText('Status for Bob Jones')).toHaveValue('LATE');

    await userEvent.selectOptions(screen.getByLabelText('Status for Alice Smith'), 'ABSENT');
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));

    expect(await screen.findByText('Attendance saved.')).toBeInTheDocument();
    const date = (screen.getByLabelText('Date') as HTMLInputElement).value;
    expect(date).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    const put = calls.find((c) => c.method === 'PUT');
    expect(put?.body).toEqual({
      date,
      records: [
        { studentId: 1, status: 'ABSENT' },
        { studentId: 2, status: 'LATE' },
      ],
    });
  });

  it('is read-only for users who cannot manage the class', async () => {
    mockFetch(({ url }) => {
      if (url === '/api/classes/7/students') return json(200, STUDENTS);
      if (url.startsWith('/api/classes/7/attendance')) return json(200, []);
    });
    render(<RollCallTab classId={7} canManage={false} />);

    expect(await screen.findByText('Alice Smith')).toBeInTheDocument();
    expect(screen.getByLabelText('Status for Alice Smith')).toBeDisabled();
    expect(screen.queryByRole('button', { name: 'Save' })).not.toBeInTheDocument();
  });
});
