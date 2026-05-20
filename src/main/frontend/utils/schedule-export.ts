import type { CourseFilter } from '../store/app-state';
import { ScheduleEndpoint } from '../generated/endpoints';

const DAY_ORDER = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'];
const LESSON_NUMBERS = [1, 2, 3, 4];
const COURSE_FILTERS = [1, 2, 3, 4] as const;

const GROUP_NAME_FONT_SIZE = 11.0;
const CURATOR_FONT_SIZE = 10.0;
const NORMAL_LESSON_FONT_SIZES = { subject: 10.0, teacher: 8.0, room: 8.0 };
const COMPACT_LESSON_FONT_SIZES = { subject: 7.0, teacher: 6.0, room: 5.8 };
const DIAGONAL_LESSON_FONT_SIZES = { subject: 8.2, teacher: 7.5, room: 7.0 };

const DAY_NAMES: Record<string, string> = {
  MONDAY: 'Понеділок',
  TUESDAY: 'Вівторок',
  WEDNESDAY: 'Середа',
  THURSDAY: 'Четвер',
  FRIDAY: 'П’ятниця'
};

type ExportOptions = {
  scheduleName?: string;
  autoPrint?: boolean;
};

const escapeHtml = (value: unknown) =>
  String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');

const uniqueText = (values: unknown[]) =>
  Array.from(new Set(values.map((value) => String(value || '').trim()).filter(Boolean)));

const formatGeneratedAt = () =>
  new Intl.DateTimeFormat('uk-UA', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date());

const fileDate = () => new Date().toISOString().slice(0, 10);

const exportFileBaseName = (scheduleName?: string) => {
  const normalized = (scheduleName || 'schedule')
    .toLowerCase()
    .replace(/[^a-zа-яіїєґ0-9]+/gi, '-')
    .replace(/^-+|-+$/g, '');
  return `${normalized || 'schedule'}-${fileDate()}`;
};

const getEffectiveCourse = (groups: any[], selectedCourse: CourseFilter): CourseFilter => {
  const courseCounts = new Map<number, number>();
  groups.forEach((group) => {
    const course = Number(group?.course);
    if (Number.isInteger(course)) {
      courseCounts.set(course, (courseCounts.get(course) || 0) + 1);
    }
  });

  const availableCourses = COURSE_FILTERS.filter((course) => courseCounts.has(course));
  return selectedCourse === 'ALL' || courseCounts.has(selectedCourse)
    ? selectedCourse
    : availableCourses[0] ?? 'ALL';
};

const getExportGroups = (data: any, selectedCourse: CourseFilter) => {
  const allGroups = [...(data?.groups || [])]
    .filter(Boolean)
    .sort((a, b) => String(a.name || '').localeCompare(String(b.name || '')));
  const effectiveCourse = getEffectiveCourse(allGroups, selectedCourse);
  const groups = effectiveCourse === 'ALL'
    ? allGroups
    : allGroups.filter((group) => Number(group.course) === effectiveCourse);

  return { allGroups, groups, effectiveCourse };
};

const getTimeslotById = (data: any) =>
  new Map<number, any>((data?.timeslots || []).filter((ts: any) => !!ts?.id).map((ts: any) => [ts.id, ts]));

const getEffectivePeriodicity = (lesson: any, timeslotById: Map<number, any>) => {
  const timeslot = timeslotById.get(lesson.timeslotId);
  return timeslot?.weekParity && timeslot.weekParity !== 'WEEKLY'
    ? timeslot.weekParity
    : lesson.periodicity;
};

const mergePairedLessons = (oddLessons: any[], evenLessons: any[], weeklyLessons: any[]) => {
  for (let i = oddLessons.length - 1; i >= 0; i--) {
    const oddLesson = oddLessons[i];
    const evenIndex = evenLessons.findIndex((lesson) => lesson.subjectName === oddLesson.subjectName);
    if (evenIndex === -1) {
      continue;
    }

    const evenLesson = evenLessons[evenIndex];
    const merged = { ...oddLesson, periodicity: 'WEEKLY' };
    if (oddLesson.teacherName !== evenLesson.teacherName && evenLesson.teacherName) {
      merged.teacherName = merged.teacherName
        ? `${merged.teacherName}, ${evenLesson.teacherName}`
        : evenLesson.teacherName;
    }
    if (oddLesson.roomName !== evenLesson.roomName && evenLesson.roomName) {
      merged.roomName = merged.roomName
        ? `${merged.roomName}, ${evenLesson.roomName}`
        : evenLesson.roomName;
    }

    weeklyLessons.push(merged);
    oddLessons.splice(i, 1);
    evenLessons.splice(evenIndex, 1);
  }
};

const renderLessonGroup = (lessons: any[], compact = false) => {
  if (!lessons.length) {
    return '';
  }

  const first = lessons[0];
  const teachers = uniqueText(lessons.map((lesson) =>
    `${lesson.teacherName || '—'}${lesson.teacherArchived ? ' (арх.)' : ''}`
  ));
  const rooms = uniqueText(lessons.map((lesson) => lesson.roomName || '—'));
  const subgroups = uniqueText(lessons
    .filter((lesson) => Number(lesson.subgroup) > 0)
    .map((lesson) => `${lesson.subgroup}-а підгр.`));

  return `
    <div class="lesson ${compact ? 'lesson-compact' : ''}">
      <div class="subject">${escapeHtml(first.subjectName || '—')}</div>
      <div class="teacher">${teachers.map(escapeHtml).join('<br>')}</div>
      <div class="room">ауд. №${rooms.map(escapeHtml).join(', ')}${subgroups.length ? ` <span class="subgroup">${subgroups.map(escapeHtml).join(', ')}</span>` : ''}</div>
    </div>
  `;
};

const renderNormalCell = (slotLessons: any[]) => {
  const groupedBySubject = Array.from(
    slotLessons.reduce((map: Map<string, any[]>, lesson: any) => {
      const key = lesson.subjectName || 'Невідома дисципліна';
      if (!map.has(key)) {
        map.set(key, []);
      }
      map.get(key)!.push(lesson);
      return map;
    }, new Map<string, any[]>())
  ) as Array<[string, any[]]>;

  if (groupedBySubject.length === 1) {
    return renderLessonGroup(groupedBySubject[0][1]);
  }

  return `
    <div class="split-lessons">
      ${groupedBySubject.map(([, lessons]) => `<div class="split-lesson">${renderLessonGroup(lessons, true)}</div>`).join('')}
    </div>
  `;
};

const renderSlotCell = (data: any, group: any, day: string, lessonNumber: number, timeslotById: Map<number, any>) => {
  const slotLessons = (data?.lessons || []).filter((lesson: any) => {
    const timeslot = timeslotById.get(lesson.timeslotId);
    return lesson.groupName === group.name &&
      timeslot?.dayOfWeek === day &&
      timeslot?.lessonNumber === lessonNumber;
  });

  if (!slotLessons.length) {
    return '<td class="slot empty"></td>';
  }

  let oddLessons = slotLessons.filter((lesson: any) => getEffectivePeriodicity(lesson, timeslotById) === 'ODD_WEEKS');
  let evenLessons = slotLessons.filter((lesson: any) => getEffectivePeriodicity(lesson, timeslotById) === 'EVEN_WEEKS');
  let weeklyLessons = slotLessons.filter((lesson: any) => getEffectivePeriodicity(lesson, timeslotById) === 'WEEKLY');

  mergePairedLessons(oddLessons, evenLessons, weeklyLessons);

  const hasSplitSubgroups = slotLessons.length > 1 && slotLessons.some((lesson: any) => Number(lesson.subgroup) > 0);
  const fallbackSplit = oddLessons.length === 0 && evenLessons.length === 0 && weeklyLessons.length > 1 && !hasSplitSubgroups;
  if (fallbackSplit) {
    oddLessons = [weeklyLessons[0]];
    evenLessons = weeklyLessons.slice(1);
    weeklyLessons = [];
  }

  const hasOdd = oddLessons.length > 0;
  const hasEven = evenLessons.length > 0;
  const hasWeekly = weeklyLessons.length > 0;
  const isDiagonal = !hasSplitSubgroups && (fallbackSplit || (!hasWeekly && (hasOdd || hasEven)));

  if (isDiagonal) {
    return `
      <td class="slot diagonal">
        <svg class="diagonal-line" viewBox="0 0 100 100" preserveAspectRatio="none" aria-hidden="true">
          <line x1="0" y1="100" x2="100" y2="0"></line>
        </svg>
        <div class="diagonal-part diagonal-top">${hasOdd ? renderLessonGroup(oddLessons, true) : ''}</div>
        <div class="diagonal-part diagonal-bottom">${hasEven ? renderLessonGroup(evenLessons, true) : ''}</div>
      </td>
    `;
  }

  return `<td class="slot">${renderNormalCell(slotLessons)}</td>`;
};

const buildDayTable = (data: any, groups: any[], day: string, timeslotById: Map<number, any>) => {
  const headerGroups = groups.map((group) => `<th class="group-name">${escapeHtml(group.name)}</th>`).join('');
  const headerCurators = groups.map((group) => `<th class="curator">${escapeHtml(group.curatorName || '—')}</th>`).join('');

  return `
    <table class="schedule-table">
      <thead>
        <tr>
          <th class="corner" colspan="2" rowspan="2"></th>
          ${headerGroups}
        </tr>
        <tr>${headerCurators}</tr>
      </thead>
      <tbody class="day-block">
        ${LESSON_NUMBERS.map((lessonNumber, index) => `
      <tr class="${index === 0 ? 'day-start' : ''}">
        ${index === 0 ? `<td class="day" rowspan="${LESSON_NUMBERS.length}"><span>${escapeHtml(DAY_NAMES[day])}</span></td>` : ''}
        <td class="lesson-number">${lessonNumber}</td>
        ${groups.map((group) => renderSlotCell(data, group, day, lessonNumber, timeslotById)).join('')}
      </tr>
    `).join('')}
      </tbody>
    </table>
  `;
};

export const buildScheduleExportHtml = (data: any, selectedCourse: CourseFilter, options: ExportOptions = {}) => {
  const { groups, effectiveCourse } = getExportGroups(data, selectedCourse);
  const timeslotById = getTimeslotById(data);
  const title = options.scheduleName?.trim() || 'Розклад занять';
  const courseLabel = effectiveCourse === 'ALL' ? 'усі курси' : `${effectiveCourse} курс`;
  const generatedAt = formatGeneratedAt();
  const printScript = options.autoPrint
    ? '<script>window.addEventListener("load", function () { setTimeout(function () { window.print(); }, 250); });</script>'
    : '';
  const renderPageHeader = () => `
    <div class="header">
      <div>
        <h1>${escapeHtml(title)}</h1>
        <div class="meta course-label">${escapeHtml(courseLabel)}</div>
      </div>
      <div class="meta">Сформовано: ${escapeHtml(generatedAt)}</div>
    </div>
  `;
  const pages = groups.length > 0
    ? DAY_ORDER.map((day) => `
  <main class="page">
    ${renderPageHeader()}
    ${buildDayTable(data, groups, day, timeslotById)}
  </main>`).join('')
    : `
  <main class="page">
    ${renderPageHeader()}
    <div class="empty-state">Немає даних розкладу для експорту</div>
  </main>`;

  return `<!doctype html>
<html lang="uk">
<head>
  <meta charset="utf-8">
  <title>${escapeHtml(title)} - ${escapeHtml(courseLabel)}</title>
  <style>
    @page { size: A4 landscape; margin: 0; }
    * { box-sizing: border-box; }
    body {
      margin: 0;
      color: #000;
      background: #e5e7eb;
      font-family: "Times New Roman", Times, serif;
    }
    .page {
      width: 297mm;
      min-height: 210mm;
      margin: 0 auto 12mm;
      padding: 8.5mm;
      background: #fff;
      page-break-after: always;
      --group-name-font-size: ${GROUP_NAME_FONT_SIZE}pt;
      --curator-font-size: ${CURATOR_FONT_SIZE}pt;
      --normal-subject-font-size: ${NORMAL_LESSON_FONT_SIZES.subject}pt;
      --normal-teacher-font-size: ${NORMAL_LESSON_FONT_SIZES.teacher}pt;
      --normal-room-font-size: ${NORMAL_LESSON_FONT_SIZES.room}pt;
      --compact-subject-font-size: ${COMPACT_LESSON_FONT_SIZES.subject}pt;
      --compact-teacher-font-size: ${COMPACT_LESSON_FONT_SIZES.teacher}pt;
      --compact-room-font-size: ${COMPACT_LESSON_FONT_SIZES.room}pt;
      --diagonal-subject-font-size: ${DIAGONAL_LESSON_FONT_SIZES.subject}pt;
      --diagonal-teacher-font-size: ${DIAGONAL_LESSON_FONT_SIZES.teacher}pt;
      --diagonal-room-font-size: ${DIAGONAL_LESSON_FONT_SIZES.room}pt;
    }
    .page:last-of-type {
      page-break-after: auto;
    }
    .header {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 24pt;
      margin-bottom: 14mm;
      border-bottom: 1.4pt solid #000;
      padding-bottom: 3mm;
    }
    h1 {
      margin: 0;
      font-family: Arial, sans-serif;
      font-size: 14pt;
      font-weight: 800;
      line-height: 1.1;
      letter-spacing: 0;
    }
    .meta {
      font-family: Arial, sans-serif;
      font-size: 7.5pt;
      color: #000;
      text-align: right;
      white-space: nowrap;
    }
    .course-label {
      margin-top: 2.5pt;
      text-align: left;
    }
    .empty-state {
      font-size: 13pt;
      margin-top: 14mm;
    }
    .schedule-table {
      width: 100%;
      height: 166.9mm;
      border-collapse: collapse;
      table-layout: fixed;
      border: 1.6pt solid #000;
      background: #fff;
    }
    thead {
      display: table-header-group;
    }
    .day-block {
      break-inside: avoid;
      page-break-inside: avoid;
    }
    tr {
      break-inside: avoid;
      page-break-inside: avoid;
    }
    th, td {
      border: 0.8pt solid #000;
      padding: 0;
      vertical-align: middle;
    }
    .corner {
      width: 18.7mm;
      border-right-width: 1.1pt;
      border-bottom-width: 1.1pt;
      background: #fff;
    }
    .group-name {
      height: 32pt;
      padding: 2pt 4pt;
      font-family: Arial, sans-serif;
      font-size: var(--group-name-font-size);
      font-weight: 800;
      text-transform: uppercase;
      text-align: center;
      border-bottom-width: 0.8pt;
    }
    .curator {
      height: 20pt;
      padding: 2pt 4pt;
      font-size: var(--curator-font-size);
      font-weight: 400;
      text-align: center;
      border-bottom-width: 1.1pt;
      white-space: nowrap;
      overflow: visible;
    }
    .day {
      width: 9.5mm;
      border-width: 1.4pt;
      text-align: center;
    }
    .day span {
      display: inline-block;
      writing-mode: vertical-rl;
      transform: rotate(180deg);
      font-size: 11pt;
      font-weight: 900;
      line-height: 1;
    }
    .lesson-number {
      width: 9.2mm;
      height: 37.1mm;
      border: 1.1pt solid #000;
      font-size: 18pt;
      font-weight: 900;
      text-align: center;
    }
    .slot {
      height: 37.1mm;
      padding: 4pt;
      position: relative;
      text-align: center;
      vertical-align: top;
      overflow: visible;
    }
    .empty { background: #fff; }
    .lesson {
      display: flex;
      min-height: 0;
      height: 100%;
      width: 100%;
      flex-direction: column;
      align-items: center;
      justify-content: flex-start;
      gap: 1pt;
      line-height: 1.08;
      overflow: visible;
    }
    .lesson-compact {
      min-height: 0;
      justify-content: flex-start;
      gap: 0;
    }
    .subject {
      max-width: 100%;
      font-size: var(--normal-subject-font-size);
      font-weight: 900;
      text-transform: uppercase;
      text-decoration: underline;
      line-height: 1.08;
      overflow: visible;
      overflow-wrap: normal;
      word-break: normal;
      hyphens: none;
      white-space: normal;
    }
    .lesson-compact .subject { font-size: var(--compact-subject-font-size); }
    .teacher {
      max-width: 100%;
      font-size: var(--normal-teacher-font-size);
      font-weight: 400;
      line-height: 1.08;
      overflow: visible;
      overflow-wrap: normal;
      word-break: normal;
      hyphens: none;
      white-space: normal;
    }
    .lesson-compact .teacher { font-size: var(--compact-teacher-font-size); }
    .room {
      max-width: 100%;
      font-size: var(--normal-room-font-size);
      font-weight: 800;
      line-height: 1.08;
      overflow: visible;
      overflow-wrap: normal;
      word-break: normal;
      hyphens: none;
      white-space: normal;
    }
    .lesson-compact .room { font-size: var(--compact-room-font-size); }
    .subgroup {
      font-style: italic;
      font-weight: 400;
    }
    .split-lessons {
      display: flex;
      height: 100%;
      min-height: 0;
      flex-direction: column;
      overflow: visible;
    }
    .split-lesson {
      flex: 1 1 0;
      min-height: 0;
      overflow: visible;
    }
    .split-lesson + .split-lesson {
      border-top: 0.45pt solid #000;
      padding-top: 1pt;
    }
    .split-lesson .lesson {
      height: auto;
    }
    .lesson-divider {
      width: 100%;
      border-top: 1px solid #000;
      margin: 2px 0;
    }
    .diagonal {
      padding: 0;
      overflow: visible;
      background: #fff;
    }
    .diagonal-line {
      position: absolute;
      inset: 0;
      z-index: 0;
      width: 100%;
      height: 100%;
      shape-rendering: geometricPrecision;
      pointer-events: none;
    }
    .diagonal-line line {
      stroke: #000;
      stroke-width: 1.1;
      stroke-linecap: square;
    }
    .diagonal-part {
      position: absolute;
      z-index: 1;
      display: flex;
      height: 38%;
      min-height: 0;
      overflow: visible;
    }
    .diagonal-top {
      top: 4pt;
      left: 4pt;
      width: 48%;
      align-items: flex-start;
      justify-content: flex-start;
      text-align: left;
    }
    .diagonal-bottom {
      right: 4pt;
      bottom: 4pt;
      width: 44%;
      align-items: flex-end;
      justify-content: flex-end;
      text-align: right;
    }
    .diagonal .lesson {
      min-height: 0;
      gap: 0;
      line-height: 1.08;
      overflow: visible;
    }
    .diagonal-top .lesson {
      align-items: flex-start;
      justify-content: flex-start;
      text-align: left;
    }
    .diagonal-bottom .lesson {
      align-items: flex-end;
      justify-content: flex-end;
      text-align: right;
    }
    .diagonal .subject {
      font-size: var(--diagonal-subject-font-size);
      line-height: 1.08;
      overflow: visible;
    }
    .diagonal .teacher {
      font-size: var(--diagonal-teacher-font-size);
      line-height: 1.08;
      overflow: visible;
    }
    .diagonal .room {
      font-size: var(--diagonal-room-font-size);
      line-height: 1.08;
      overflow: visible;
    }
    @media print {
      body {
        background: #fff;
        -webkit-print-color-adjust: exact;
        print-color-adjust: exact;
      }
      .page {
        width: 297mm;
        min-height: 210mm;
        margin: 0;
        padding: 8.5mm;
        page-break-after: always;
      }
      .page:last-of-type { page-break-after: auto; }
    }
  </style>
</head>
<body>
  ${pages}
  ${printScript}
</body>
</html>`;
};

export const downloadScheduleHtml = (data: any, selectedCourse: CourseFilter, scheduleName?: string) => {
  const html = buildScheduleExportHtml(data, selectedCourse, { scheduleName });
  const blob = new Blob([html], { type: 'text/html;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `${exportFileBaseName(scheduleName)}.html`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.setTimeout(() => URL.revokeObjectURL(url), 1000);
};

export const downloadSchedulePdf = async (data: any, selectedCourse: CourseFilter, scheduleName?: string) => {
  const base64 = await (ScheduleEndpoint as any).exportSchedulePdf(String(selectedCourse), scheduleName || 'Розклад занять');
  const binary = atob(base64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  const blob = new Blob([bytes], { type: 'application/pdf' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `${exportFileBaseName(scheduleName)}.pdf`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.setTimeout(() => URL.revokeObjectURL(url), 1000);
};
