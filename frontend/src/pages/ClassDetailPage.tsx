/*
 * Author: Jamius Siam
 * Since: 18/11/2026
 */
import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api/client';
import type { SchoolClass } from '../api/types';
import { useUser } from '../auth/AuthContext';
import ErrorBox from '../components/ErrorBox';
import ClassStudentsTab from './class/ClassStudentsTab';
import RollCallTab from './class/RollCallTab';
import GradesTab from './class/GradesTab';

type Tab = 'students' | 'attendance' | 'grades';

export default function ClassDetailPage() {
  const { id } = useParams();
  const classId = Number(id);
  const user = useUser();
  const [cls, setCls] = useState<SchoolClass | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [tab, setTab] = useState<Tab>('students');

  useEffect(() => {
    api<SchoolClass>(`/classes/${classId}`).then(setCls).catch(setError);
  }, [classId]);

  if (error) return <ErrorBox error={error} />;
  if (!cls) return <p className="muted">Loading...</p>;

  // Admin or the class teacher may change enrollment, attendance and grades.
  const canManage = user.role === 'ADMIN' || (user.role === 'TEACHER' && user.teacherId === cls.teacherId);
  // Enrollment, roll call and grade endpoints are ADMIN/TEACHER only.
  const showTabs = user.role !== 'STUDENT';

  return (
    <div>
      <p>
        <Link to="/classes">&larr; Classes</Link>
      </p>
      <h1>{cls.name}</h1>
      <div className="card">
        Grade {cls.gradeLevel} {cls.section && `- Section ${cls.section}`} | {cls.academicYear} | Teacher:{' '}
        {cls.teacherName ?? '-'} | {cls.studentCount} students
      </div>

      {showTabs && (
        <>
          <div className="tabs">
            <button className={tab === 'students' ? 'active' : ''} onClick={() => setTab('students')}>
              Students
            </button>
            <button className={tab === 'attendance' ? 'active' : ''} onClick={() => setTab('attendance')}>
              Roll call
            </button>
            <button className={tab === 'grades' ? 'active' : ''} onClick={() => setTab('grades')}>
              Grades
            </button>
          </div>
          {tab === 'students' && <ClassStudentsTab classId={classId} canManage={canManage} />}
          {tab === 'attendance' && <RollCallTab classId={classId} canManage={canManage} />}
          {tab === 'grades' && <GradesTab classId={classId} canManage={canManage} />}
        </>
      )}
    </div>
  );
}
