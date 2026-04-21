import React, { useState } from 'react';
import type LessonDTO from '../generated/com/sergofoox/domain/ui/dto/LessonDTO';
import { ReplacementDialog } from './ReplacementDialog';
import { Button } from '@vaadin/react-components/Button.js';
import { isPublished } from '../store/app-state';

interface GridCellProps {
  lesson?: LessonDTO;
  mode: 'GROUP' | 'TEACHER' | 'ROOM';
  onDragStart?: (e: React.DragEvent, lessonId: number) => void;
  onDrop?: (e: React.DragEvent) => void;
  onDragOver?: (e: React.DragEvent) => void;
}

export const GridCell: React.FC<GridCellProps> = ({ lesson, mode, onDragStart, onDrop, onDragOver }) => {
  const [dialogOpened, setDialogOpened] = useState(false);
  const isConflict = lesson?.hasConflict;
  const published = isPublished.value;

  if (!lesson) {
    return (
      <div 
        onDrop={onDrop}
        onDragOver={onDragOver}
        className="p-2 text-gray-500 text-xs flex items-center justify-center h-full w-full transition-colors hover:bg-gray-50" 
        style={{ 
          minHeight: '60px', 
          border: '1px dashed var(--aura-contrast-10)', 
          borderRadius: 'var(--aura-border-radius)' 
        }}
      >
        -
      </div>
    );
  }

  return (
    <>
      <div 
        draggable={!published}
        onDragStart={(e) => onDragStart?.(e, lesson.id as any)}
        onDrop={onDrop}
        onDragOver={onDragOver}
        className={`p-2 shadow-sm border rounded-md flex flex-col gap-1 h-full w-full ${published ? '' : 'cursor-move'} transition-all group ${isConflict ? 'animate-pulse' : ''}`}
        style={{ 
          minHeight: '60px', 
          border: `1px solid ${isConflict ? 'var(--aura-error-color)' : 'var(--aura-accent-border-color)'}`,
          backgroundColor: isConflict ? 'rgba(var(--aura-error-color-rgb), 0.05)' : 'var(--aura-surface-color)',
          boxShadow: isConflict ? '0 0 0 1px var(--aura-error-color)' : 'var(--aura-box-shadow-s)'
        }}
      >
        <div className="flex justify-between items-start gap-1">
          <div className="font-bold text-s" style={{ color: isConflict ? 'var(--aura-error-color)' : 'var(--aura-primary-text-color)' }}>
            {lesson.subjectName}
            {isConflict && <span className="ml-1" title="Конфлікт! Перевірте накладки викладача/групи/аудиторії">⚠️</span>}
          </div>
          {!published && (
            <Button 
              theme="tertiary-inline small" 
              className="opacity-0 group-hover:opacity-100 transition-opacity"
              onClick={(e) => {
                e.stopPropagation();
                setDialogOpened(true);
              }}
              title="Замінити викладача"
            >
              🔄
            </Button>
          )}
        </div>
        <div className="text-xs text-gray-500" style={{ color: 'var(--aura-secondary-text-color)' }}>
          {mode === 'GROUP' && <span>{lesson.teacherName} • {lesson.roomName}</span>}
          {mode === 'TEACHER' && <span>{lesson.groupName} • {lesson.roomName}</span>}
          {mode === 'ROOM' && <span>{lesson.groupName} • {lesson.teacherName}</span>}
        </div>
      </div>

      <ReplacementDialog 
        lesson={lesson} 
        opened={dialogOpened} 
        onClose={() => setDialogOpened(false)} 
      />
    </>
  );
};
