const roomTypeLabels: Record<string, string> = {
  GENERAL_CLASSROOM: 'Загальна аудиторія',
  LECTURE_HALL: 'Лекційна аудиторія',
  LABORATORY: 'Лабораторія',
  COMPUTER_CLASS: 'Компʼютерний клас',
  SPORTS_HALL: 'Спортивний зал'
};

const positionTypeLabels: Record<string, string> = {
  FULL_TIME: 'Штатний викладач',
  PART_TIME: 'Сумісник',
  CONTRACT: 'За договором'
};

const lessonTypeLabels: Record<string, string> = {
  LECTURE: 'Лекція',
  PRACTICE: 'Практика',
  LABORATORY: 'Лабораторна'
};

const priorityLabels: Record<string, string> = {
  PRIMARY: 'Основний',
  SECONDARY: 'Додатковий',
  SUBSTITUTE: 'Заміна'
};

const formatLabel = (labels: Record<string, string>, value?: string | null) => {
  if (!value) return '-';
  return labels[value] || value;
};

export const formatRoomType = (value?: string | null) => formatLabel(roomTypeLabels, value);
export const formatPositionType = (value?: string | null) => formatLabel(positionTypeLabels, value);
export const formatLessonType = (value?: string | null) => formatLabel(lessonTypeLabels, value);
export const formatPriority = (value?: string | null) => formatLabel(priorityLabels, value);
