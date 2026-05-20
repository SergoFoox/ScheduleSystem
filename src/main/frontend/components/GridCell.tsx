import React, { useState } from 'react';
import { Button } from '@vaadin/react-components/Button.js';
import { Icon } from '@vaadin/react-components/Icon.js';
import { Notification } from '@vaadin/react-components/Notification.js';
import { ReplacementDialog } from './ReplacementDialog';
import { BASE_TEMPLATE_LOCKED_MESSAGE, isBaseTemplateLocked } from '../store/app-state';

interface GridCellProps {
  lessons: any[]; 
  mode: 'GROUP' | 'TEACHER' | 'ROOM';
  onDragStart?: (e: React.DragEvent, id: number) => void;
  compact?: boolean;
  align?: 'left' | 'right' | 'center';
  suppressConflictIndicator?: boolean;
}

export const GridCell: React.FC<GridCellProps> = ({ lessons, mode, onDragStart, compact, align = 'center', suppressConflictIndicator = false }) => {
  const [activeLesson, setActiveLesson] = useState<any>(null);
  const [dialogOpened, setDialogOpened] = useState(false);
  
  if (!lessons || lessons.length === 0) return null;

  const first = lessons[0];
  const hasRealConflict = !suppressConflictIndicator && lessons.some((l: any) => l.hasConflict);
  const baseTemplateLocked = isBaseTemplateLocked.value;
  const hasSplitSubgroups = lessons.length > 1 && lessons.some((l: any) => l.subgroup > 0);

  const handleReplacement = (e: React.MouseEvent, lesson: any) => {
    e.stopPropagation(); 
    if (baseTemplateLocked) {
      Notification.show(BASE_TEMPLATE_LOCKED_MESSAGE, { theme: 'primary', position: 'bottom-end' });
      return;
    }
    setActiveLesson(lesson);
    setDialogOpened(true);
  };

  const subjectFontSize = compact ? 'text-[12px]' : 'text-[14px]';
  const teacherFontSize = compact ? 'text-[11px]' : 'text-[13px]';
  const roomFontSize = compact ? 'text-[10px]' : 'text-[12px]';

  const alignClasses = align === 'left' ? 'items-start text-left' : 
                       align === 'right' ? 'items-end text-right' : 
                       'items-center text-center';

  // Collect unique room numbers.
  const uniqueRooms = Array.from(new Set(lessons.map(l => l.roomName || '—'))).join(', ');

  const displayLessons = hasSplitSubgroups
    ? lessons.map((lesson) => ({ ...lesson, teacherName: lesson.teacherName || uniqueRooms }))
    : lessons;

  return (
    <>
      <div 
        draggable={!baseTemplateLocked && !!onDragStart}
        onDragStart={(e) => {
          if (baseTemplateLocked || !onDragStart) {
            e.preventDefault();
            if (baseTemplateLocked) {
              Notification.show(BASE_TEMPLATE_LOCKED_MESSAGE, { theme: 'primary', position: 'bottom-end' });
            }
            return;
          }
          e.stopPropagation();
          onDragStart(e, first.id);
        }}
        onClick={(e) => handleReplacement(e, first)}
        className={`w-full flex flex-col justify-center relative group cursor-pointer hover:bg-black/5 rounded transition-colors ${hasRealConflict ? 'bg-red-50' : 'bg-transparent'} ${alignClasses} p-1.5 leading-[1.1]`}
      >
        <div className={`flex flex-col gap-0.5 w-full ${alignClasses}`}>
          {/* Subject */}
          <div className={`flex items-center gap-1 w-full relative mb-0.5 ${align === 'right' ? 'justify-end' : align === 'left' ? 'justify-start' : 'justify-center'}`}>
            <span className={`${subjectFontSize} font-black font-serif uppercase underline decoration-1 underline-offset-1 ${hasRealConflict ? 'text-red-700' : 'text-black'}`} title={first.subjectName}>
              {first.subjectName}
            </span>
          </div>
          
          {/* Teachers List */}
          <div className={`flex flex-col gap-0 w-full ${alignClasses}`}>
            {displayLessons.map((l) => (
              <div key={l.id} className={`relative group/teacher flex items-center w-full px-1 ${align === 'right' ? 'justify-end' : align === 'left' ? 'justify-start' : 'justify-center'}`}>
                {align === 'right' && (
                   <Button 
                    theme="tertiary-inline small" 
                    className="opacity-0 group-hover/teacher:opacity-100 p-0 h-4 w-4 min-w-0 transition-opacity bg-blue-600 text-white rounded shadow-sm z-20 mr-1"
                    onClick={(e) => handleReplacement(e, l)}
                  >
                    <Icon icon="vaadin:edit" className="w-3 h-3" />
                  </Button>
                )}
                
                <span className={`${teacherFontSize} font-normal font-serif text-black truncate flex items-center gap-1`}>
                  {l.teacherName || '—'}
                  {l.teacherArchived && (
                    <Icon 
                      icon="vaadin:package" 
                      className="w-3 h-3 text-gray-400" 
                      title="Цей викладач в архіві" 
                    />
                  )}
                </span>
                
                {(align === 'left' || align === 'center') && (
                  <Button 
                    theme="tertiary-inline small" 
                    className="ml-1 opacity-0 group-hover/teacher:opacity-100 p-0 h-4 w-4 min-w-0 transition-opacity bg-blue-600 text-white rounded shadow-sm z-20"
                    onClick={(e) => handleReplacement(e, l)}
                  >
                    <Icon icon="vaadin:edit" className="w-3 h-3" />
                  </Button>
                )}
              </div>
            ))}
          </div>
          
          {/* Combined Rooms and Conditional Subgroup label */}
          <div className={`${roomFontSize} text-black font-bold font-serif flex flex-col ${alignClasses} gap-0 mt-0.5`}>
            <div className={`flex flex-wrap ${align === 'right' ? 'justify-end' : align === 'left' ? 'justify-start' : 'justify-center'} leading-tight`}>
               <span>ауд. №{uniqueRooms}</span>
               {/* Show the subgroup only when there is exactly one in the cell. */}
               {!hasSplitSubgroups && lessons.length === 1 && first.subgroup > 0 && (
                 <span className="ml-1 italic font-normal text-[0.9em] whitespace-nowrap">
                   {first.subgroup}-а підгр.
                 </span>
               )}
            </div>
          </div>
        </div>
        
        {hasRealConflict && (
          <div className="absolute top-0 right-0 -mt-1 -mr-1 z-30">
             <span className="flex h-3 w-3 relative">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-red-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-3 w-3 bg-red-500 text-[8px] text-white items-center justify-center font-bold">!</span>
            </span>
          </div>
        )}
      </div>

      {activeLesson && (
        <ReplacementDialog 
          lesson={activeLesson} 
          opened={dialogOpened} 
          onClose={() => {
            setDialogOpened(false);
            setActiveLesson(null);
          }} 
        />
      )}
    </>
  );
};
