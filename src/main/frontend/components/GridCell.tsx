import React, { useState } from 'react';
import { Button } from '@vaadin/react-components/Button.js';
import { Icon } from '@vaadin/react-components/Icon.js';
import { ReplacementDialog } from './ReplacementDialog';
import { isPublished } from '../store/app-state';

interface GridCellProps {
  lesson?: any;
  mode: 'GROUP' | 'TEACHER' | 'ROOM';
  onDragStart?: (e: React.DragEvent, id: number) => void;
  onDrop?: (e: React.DragEvent) => void;
  onDragOver?: (e: React.DragEvent) => void;
}

export const GridCell: React.FC<GridCellProps> = ({ lesson, mode, onDragStart, onDrop, onDragOver }) => {
  const [dialogOpened, setDialogOpened] = useState(false);
  const isConflict = lesson?.hasConflict;
  const published = isPublished.value;

  if (!lesson) return null;

  return (
    <>
      <div 
        draggable={!published}
        onDragStart={(e) => onDragStart?.(e, lesson.id)}
        onDrop={onDrop}
        onDragOver={onDragOver}
        className={`h-full w-full p-1 flex flex-col justify-center items-center text-center relative group ${isConflict ? 'bg-red-50' : 'bg-transparent'}`}
      >
        {lesson.subgroup > 0 && (
          <div className="absolute top-0 left-0 border-r border-b border-black px-1 text-[10px] font-black bg-white">
            {lesson.subgroup}
          </div>
        )}

        <div className="flex flex-col gap-0 items-center justify-center w-full">
          <div className="flex items-center justify-center gap-1 w-full relative">
            <span className={`text-[16px] font-black leading-tight uppercase underline decoration-2 underline-offset-2 ${isConflict ? 'text-red-700' : 'text-black'}`} title={lesson.subjectName}>
              {lesson.subjectName}
            </span>
          </div>
          
          <span className="text-[14px] font-bold text-black leading-tight mt-1" title={lesson.teacherName}>
            {lesson.teacherName}
          </span>
          
          <div className="flex items-center justify-center gap-1 mt-1 text-[13px] text-black font-bold">
            <span className="font-bold">
              {lesson.roomName ? `ауд.№${lesson.roomName}` : '—'}
            </span>
          </div>
        </div>

        {!published && (
          <Button 
            theme="tertiary-inline small" 
            className="absolute bottom-0 right-0 opacity-0 group-hover:opacity-100 p-0 h-4 w-4 min-w-0 transition-opacity m-1"
            onClick={(e) => {
              e.stopPropagation();
              setDialogOpened(true);
            }}
            title="Замінити"
          >
            <Icon icon="vaadin:refresh" className="w-3 h-3 text-gray-500" />
          </Button>
        )}
        
        {isConflict && (
          <div className="absolute top-0 right-0 -mt-1 -mr-1">
             <span className="flex h-3 w-3 relative">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-red-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-3 w-3 bg-red-500 text-[8px] text-white items-center justify-center font-bold">!</span>
            </span>
          </div>
        )}
      </div>

      <ReplacementDialog 
        lesson={lesson} 
        opened={dialogOpened} 
        onClose={() => setDialogOpened(false)} 
      />
    </>
  );
};
