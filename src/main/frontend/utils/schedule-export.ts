import type { CourseFilter } from '../store/app-state';
import { ScheduleEndpoint } from '../generated/endpoints';

const DAY_ORDER = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'];
const LESSON_NUMBERS = [1, 2, 3, 4];
const COURSE_FILTERS = [1, 2, 3, 4] as const;

const DAY_NAMES: Record<string, string> = {
  MONDAY: 'понеділок',
  TUESDAY: 'вівторок',
  WEDNESDAY: 'середа',
  THURSDAY: 'четвер',
  FRIDAY: 'п’ятниця'
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

  return groupedBySubject
    .map(([, lessons], index) => `${index > 0 ? '<div class="lesson-divider"></div>' : ''}${renderLessonGroup(lessons)}`)
    .join('');
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

const buildScheduleTable = (data: any, selectedCourse: CourseFilter) => {
  const { groups, effectiveCourse } = getExportGroups(data, selectedCourse);
  const timeslotById = getTimeslotById(data);

  const headerGroups = groups.map((group) => `<th class="group-name">${escapeHtml(group.name)}</th>`).join('');
  const headerCurators = groups.map((group) => `<th class="curator">${escapeHtml(group.curatorName || '—')}</th>`).join('');
  const dayBlocks = DAY_ORDER.map((day) => `
    <tbody class="day-block">
      ${LESSON_NUMBERS.map((lessonNumber, index) => `
      <tr class="${index === 0 ? 'day-start' : ''}">
        ${index === 0 ? `<td class="day" rowspan="${LESSON_NUMBERS.length}"><span>${escapeHtml(DAY_NAMES[day])}</span></td>` : ''}
        <td class="lesson-number">${lessonNumber}</td>
        ${groups.map((group) => renderSlotCell(data, group, day, lessonNumber, timeslotById)).join('')}
      </tr>
    `).join('')}
    </tbody>
  `).join('');

  return {
    effectiveCourse,
    groupCount: groups.length,
    table: `
      <table class="schedule-table">
        <thead>
          <tr>
            <th class="corner" colspan="2" rowspan="2"></th>
            ${headerGroups}
          </tr>
          <tr>${headerCurators}</tr>
        </thead>
        ${dayBlocks}
      </table>
    `
  };
};

export const buildScheduleExportHtml = (data: any, selectedCourse: CourseFilter, options: ExportOptions = {}) => {
  const { effectiveCourse, groupCount, table } = buildScheduleTable(data, selectedCourse);
  const title = options.scheduleName?.trim() || 'Розклад занять';
  const courseLabel = effectiveCourse === 'ALL' ? 'усі курси' : `${effectiveCourse} курс`;
  const printScript = options.autoPrint
    ? '<script>window.addEventListener("load", function () { setTimeout(function () { window.print(); }, 250); });</script>'
    : '';

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
      background: #fff;
      font-family: "Times New Roman", Times, serif;
    }
    .page {
      padding: 18px;
      min-width: ${Math.max(900, 120 + groupCount * 128)}px;
    }
    .header {
      display: flex;
      align-items: flex-end;
      justify-content: space-between;
      gap: 24px;
      margin-bottom: 14px;
      border-bottom: 2px solid #111;
      padding-bottom: 10px;
    }
    h1 {
      margin: 0;
      font-family: Arial, sans-serif;
      font-size: 24px;
      line-height: 1.1;
      letter-spacing: 0;
    }
    .meta {
      font-family: Arial, sans-serif;
      font-size: 12px;
      color: #444;
      text-align: right;
      white-space: nowrap;
    }
    .schedule-table {
      width: 100%;
      border-collapse: collapse;
      table-layout: fixed;
      border: 3px solid #000;
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
      border: 1px solid #000;
      padding: 0;
      vertical-align: middle;
    }
    .corner {
      width: 72px;
      border-right-width: 3px;
      border-bottom-width: 3px;
      background: #fff;
    }
    .group-name {
      min-width: 126px;
      height: 42px;
      padding: 6px 8px;
      font-family: Arial, sans-serif;
      font-size: 17px;
      font-weight: 800;
      text-transform: uppercase;
      text-align: center;
      border-bottom-width: 1px;
    }
    .curator {
      height: 28px;
      padding: 4px 6px;
      font-size: 12px;
      font-weight: 400;
      text-align: center;
      border-bottom-width: 3px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .day {
      width: 42px;
      border-right-width: 3px;
      border-bottom-width: 3px;
      text-align: center;
    }
    .day span {
      display: inline-block;
      writing-mode: vertical-rl;
      transform: rotate(180deg);
      font-size: 18px;
      font-weight: 900;
      line-height: 1;
      text-transform: lowercase;
    }
    .lesson-number {
      width: 34px;
      height: 88px;
      border: 3px solid #000;
      font-size: 28px;
      font-weight: 900;
      text-align: center;
    }
    .slot {
      height: 92px;
      min-width: 126px;
      padding: 4px;
      position: relative;
      text-align: center;
      vertical-align: top;
      overflow: visible;
    }
    tr:nth-child(4n) .slot {
      border-bottom-width: 3px;
    }
    .empty { background: #fff; }
    .lesson {
      display: flex;
      min-height: 76px;
      width: 100%;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 2px;
      line-height: 1.08;
      overflow: visible;
    }
    .lesson-compact {
      min-height: 34px;
      align-items: flex-start;
      justify-content: flex-start;
      text-align: left;
      gap: 0;
    }
    .subject {
      max-width: 100%;
      font-size: 13px;
      font-weight: 900;
      text-transform: uppercase;
      text-decoration: underline;
      line-height: 1.05;
      overflow: visible;
      overflow-wrap: anywhere;
      word-break: break-word;
      white-space: normal;
    }
    .lesson-compact .subject { font-size: 9.5px; }
    .teacher {
      max-width: 100%;
      font-size: 12px;
      font-weight: 400;
      line-height: 1.05;
      overflow: visible;
      overflow-wrap: anywhere;
      word-break: break-word;
      white-space: normal;
    }
    .lesson-compact .teacher { font-size: 8.5px; }
    .room {
      max-width: 100%;
      font-size: 11px;
      font-weight: 800;
      line-height: 1.05;
      overflow: visible;
      overflow-wrap: anywhere;
      word-break: break-word;
      white-space: normal;
    }
    .lesson-compact .room { font-size: 8px; }
    .subgroup {
      font-style: italic;
      font-weight: 400;
    }
    .lesson-divider {
      width: 100%;
      border-top: 1px solid #000;
      margin: 2px 0;
    }
    .diagonal {
      padding: 0;
      overflow: hidden;
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
      stroke: #555;
      stroke-width: 1.35;
      stroke-linecap: square;
    }
    .diagonal-part {
      position: absolute;
      z-index: 1;
      display: flex;
      width: 58%;
      height: 42%;
      min-height: 0;
      overflow: hidden;
    }
    .diagonal-top {
      top: 3px;
      left: 4px;
      align-items: flex-start;
      justify-content: flex-start;
      text-align: left;
    }
    .diagonal-bottom {
      right: 4px;
      bottom: 3px;
      align-items: flex-end;
      justify-content: flex-end;
      text-align: right;
    }
    .diagonal .lesson {
      min-height: 0;
      gap: 0;
      line-height: 1;
      overflow: hidden;
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
      font-size: 8.8px;
      line-height: 1;
      max-height: 2.15em;
      overflow: hidden;
    }
    .diagonal .teacher {
      font-size: 7.8px;
      line-height: 1;
      max-height: 2.05em;
      overflow: hidden;
    }
    .diagonal .room {
      font-size: 7.4px;
      line-height: 1;
      max-height: 2.05em;
      overflow: hidden;
    }
    @media print {
      body { -webkit-print-color-adjust: exact; print-color-adjust: exact; }
      .page {
        width: 297mm;
        min-height: 210mm;
        padding: 9mm;
        min-width: 0;
      }
      .header {
        margin-bottom: 7mm;
        padding-bottom: 3mm;
      }
      h1 { font-size: 20px; }
      .meta { font-size: 10px; }
      .group-name { font-size: 13px; }
      .subject { font-size: 9px; }
      .teacher { font-size: 9px; }
      .room { font-size: 8px; }
      .lesson-compact .subject { font-size: 8.8px; }
      .lesson-compact .teacher { font-size: 7.8px; }
      .lesson-compact .room { font-size: 7.3px; }
      .diagonal .subject { font-size: 8.2px; }
      .diagonal .teacher { font-size: 7.3px; }
      .diagonal .room { font-size: 6.9px; }
      .slot { height: 84px; }
      .lesson-number { height: 84px; font-size: 24px; }
    }
  </style>
</head>
<body>
  <main class="page">
    <div class="header">
      <div>
        <h1>${escapeHtml(title)}</h1>
        <div class="meta" style="text-align:left;margin-top:4px;">${escapeHtml(courseLabel)}</div>
      </div>
      <div class="meta">Сформовано: ${escapeHtml(formatGeneratedAt())}</div>
    </div>
    ${table}
  </main>
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
