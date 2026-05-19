package com.sergofoox.domain.ui;

import com.sergofoox.domain.plan.Periodicity;
import com.sergofoox.domain.ui.dto.GroupDTO;
import com.sergofoox.domain.ui.dto.LessonDTO;
import com.sergofoox.domain.ui.dto.ScheduleGridDTO;
import com.sergofoox.domain.ui.dto.TimeslotDTO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.util.Matrix;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Collator;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SchedulePdfExportService {

    private static final List<DayOfWeek> DAY_ORDER = List.of(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
    );
    private static final List<Integer> LESSON_NUMBERS = List.of(1, 2, 3, 4);
    private static final Map<DayOfWeek, String> DAY_NAMES = Map.of(
            DayOfWeek.MONDAY, "Понеділок",
            DayOfWeek.TUESDAY, "Вівторок",
            DayOfWeek.WEDNESDAY, "Середа",
            DayOfWeek.THURSDAY, "Четвер",
            DayOfWeek.FRIDAY, "П’ятниця"
    );
    private static final Locale UKRAINIAN = Locale.forLanguageTag("uk-UA");
    private static final DateTimeFormatter GENERATED_AT_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm");
    private static final float GROUP_NAME_FONT_SIZE = 11.0f;
    private static final float CURATOR_FONT_SIZE = 10.0f;
    private static final LessonTextSizes NORMAL_LESSON_FONT_SIZES = new LessonTextSizes(10.0f, 8.0f, 8.0f);
    private static final LessonTextSizes COMPACT_LESSON_FONT_SIZES = new LessonTextSizes(7.0f, 6.0f, 5.8f);
    private static final LessonTextSizes DIAGONAL_LESSON_FONT_SIZES = new LessonTextSizes(8.2f, 7.5f, 7.0f);
    private static final int SUBJECT_MAX_LINES = 4;
    private static final int TEACHER_MAX_LINES = 2;
    private static final int ROOM_MAX_LINES = 2;

    public byte[] exportPdf(ScheduleGridDTO data, String selectedCourse, String scheduleName) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PdfFonts fonts = loadFonts(document);
            List<GroupDTO> groups = getExportGroups(data, selectedCourse);
            Map<Long, TimeslotDTO> timeslotsById = (data.timeslots() == null ? List.<TimeslotDTO>of() : data.timeslots())
                    .stream()
                    .filter(timeslot -> timeslot.id() != null)
                    .collect(Collectors.toMap(TimeslotDTO::id, timeslot -> timeslot, (a, b) -> a));
            Map<SlotKey, List<LessonDTO>> lessonsBySlot = groupLessonsBySlot(data, timeslotsById);

            String title = cleanText(scheduleName).isBlank() ? "Розклад занять" : cleanText(scheduleName);
            String courseLabel = courseLabel(selectedCourse);

            if (groups.isEmpty()) {
                drawEmptyPage(document, fonts, title, courseLabel);
            } else {
                for (DayOfWeek day : DAY_ORDER) {
                    drawDayPage(document, fonts, title, courseLabel, groups, day, lessonsBySlot, timeslotsById);
                }
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        }
    }

    private void drawEmptyPage(PDDocument document, PdfFonts fonts, String title, String courseLabel) throws IOException {
        PDPage page = new PDPage(landscapeA4());
        document.addPage(page);
        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            PageMetrics metrics = new PageMetrics(page);
            drawHeader(content, fonts, metrics, title, courseLabel);
            drawTextLine(content, "Немає даних розкладу для експорту", fonts.serifRegular(), 13,
                    metrics.margin(), metrics.pageHeight() - metrics.margin() - 82, false, Align.LEFT);
        }
    }

    private void drawDayPage(PDDocument document,
                             PdfFonts fonts,
                             String title,
                             String courseLabel,
                             List<GroupDTO> groups,
                             DayOfWeek day,
                             Map<SlotKey, List<LessonDTO>> lessonsBySlot,
                             Map<Long, TimeslotDTO> timeslotsById) throws IOException {
        PDPage page = new PDPage(landscapeA4());
        document.addPage(page);

        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            PageMetrics metrics = new PageMetrics(page);
            drawHeader(content, fonts, metrics, title, courseLabel);

            float tableX = metrics.margin();
            float tableTop = metrics.pageHeight() - metrics.margin() - 74;
            float tableWidth = metrics.pageWidth() - metrics.margin() * 2;
            float tableHeight = tableTop - metrics.margin();

            float dayWidth = 27;
            float numberWidth = 26;
            float groupWidth = (tableWidth - dayWidth - numberWidth) / groups.size();
            float groupHeaderHeight = 32;
            float curatorHeaderHeight = 20;
            float rowHeight = (tableHeight - groupHeaderHeight - curatorHeaderHeight) / LESSON_NUMBERS.size();
            float bodyTop = tableTop - groupHeaderHeight - curatorHeaderHeight;
            float tableBottom = metrics.margin();

            drawTableFrame(content, tableX, tableBottom, tableWidth, tableHeight);
            drawHeaders(content, fonts, groups, tableX, tableTop, dayWidth, numberWidth, groupWidth,
                    groupHeaderHeight, curatorHeaderHeight);
            drawDayLabel(content, fonts, DAY_NAMES.get(day), tableX, tableBottom, dayWidth,
                    rowHeight * LESSON_NUMBERS.size());

            for (int rowIndex = 0; rowIndex < LESSON_NUMBERS.size(); rowIndex++) {
                int lessonNumber = LESSON_NUMBERS.get(rowIndex);
                float rowTop = bodyTop - rowIndex * rowHeight;
                float rowBottom = rowTop - rowHeight;
                float numberX = tableX + dayWidth;

                drawCellBorder(content, numberX, rowBottom, numberWidth, rowHeight, 1.1f);
                drawCenteredText(content, String.valueOf(lessonNumber), fonts.serifBold(), 18,
                        numberX, rowBottom, numberWidth, rowHeight);

                for (int groupIndex = 0; groupIndex < groups.size(); groupIndex++) {
                    GroupDTO group = groups.get(groupIndex);
                    float cellX = tableX + dayWidth + numberWidth + groupIndex * groupWidth;
                    List<LessonDTO> slotLessons = lessonsBySlot.getOrDefault(
                            new SlotKey(group.id(), day, lessonNumber),
                            List.of()
                    );
                    drawSlot(content, fonts, slotLessons, timeslotsById, cellX, rowBottom, groupWidth, rowHeight);
                }
            }
        }
    }

    private void drawHeader(PDPageContentStream content,
                            PdfFonts fonts,
                            PageMetrics metrics,
                            String title,
                            String courseLabel) throws IOException {
        float left = metrics.margin();
        float top = metrics.pageHeight() - metrics.margin();
        drawTextLine(content, title, fonts.sansBold(), 14, left, top - 10, false, Align.LEFT);
        drawTextLine(content, courseLabel, fonts.sansRegular(), 7.5f, left, top - 23, false, Align.LEFT);
        drawTextLine(content, "Сформовано: " + GENERATED_AT_FORMAT.format(LocalDateTime.now()), fonts.sansRegular(), 7.5f,
                metrics.pageWidth() - metrics.margin(), top - 22, false, Align.RIGHT);
        content.setLineWidth(1.4f);
        content.moveTo(left, top - 34);
        content.lineTo(metrics.pageWidth() - metrics.margin(), top - 34);
        content.stroke();
    }

    private void drawTableFrame(PDPageContentStream content, float x, float y, float width, float height) throws IOException {
        content.setLineWidth(1.6f);
        content.addRect(x, y, width, height);
        content.stroke();
    }

    private void drawHeaders(PDPageContentStream content,
                             PdfFonts fonts,
                             List<GroupDTO> groups,
                             float tableX,
                             float tableTop,
                             float dayWidth,
                             float numberWidth,
                             float groupWidth,
                             float groupHeaderHeight,
                             float curatorHeaderHeight) throws IOException {
        float cornerWidth = dayWidth + numberWidth;
        float headerBottom = tableTop - groupHeaderHeight - curatorHeaderHeight;

        drawCellBorder(content, tableX, headerBottom, cornerWidth, groupHeaderHeight + curatorHeaderHeight, 1.1f);
        for (int i = 0; i < groups.size(); i++) {
            GroupDTO group = groups.get(i);
            float x = tableX + cornerWidth + i * groupWidth;
            drawCellBorder(content, x, tableTop - groupHeaderHeight, groupWidth, groupHeaderHeight, 0.8f);
            drawCellBorder(content, x, headerBottom, groupWidth, curatorHeaderHeight, 0.8f);
            drawCenteredText(content, cleanText(group.name()).toUpperCase(UKRAINIAN), fonts.sansBold(), GROUP_NAME_FONT_SIZE,
                    x + 2, tableTop - groupHeaderHeight, groupWidth - 4, groupHeaderHeight);
            drawCenteredText(content, emptyDash(group.curatorName()), fonts.serifRegular(), CURATOR_FONT_SIZE,
                    x + 2, headerBottom, groupWidth - 4, curatorHeaderHeight);
        }

        content.setLineWidth(1.5f);
        content.moveTo(tableX, headerBottom);
        content.lineTo(tableX + cornerWidth + groupWidth * groups.size(), headerBottom);
        content.stroke();
    }

    private void drawDayLabel(PDPageContentStream content,
                              PdfFonts fonts,
                              String label,
                              float x,
                              float y,
                              float width,
                              float height) throws IOException {
        drawCellBorder(content, x, y, width, height, 1.4f);

        String text = cleanText(label);
        float fontSize = 11;
        float textWidth = textWidth(text, fonts.serifBold(), fontSize);
        float baselineX = x + width / 2 + fontSize / 3;
        float baselineY = y + (height - textWidth) / 2;

        content.beginText();
        content.setFont(fonts.serifBold(), fontSize);
        content.setTextMatrix(new Matrix(0, 1, -1, 0, baselineX, baselineY));
        content.showText(text);
        content.endText();
    }

    private void drawSlot(PDPageContentStream content,
                          PdfFonts fonts,
                          List<LessonDTO> slotLessons,
                          Map<Long, TimeslotDTO> timeslotsById,
                          float x,
                          float y,
                          float width,
                          float height) throws IOException {
        drawCellBorder(content, x, y, width, height, 0.75f);
        if (slotLessons == null || slotLessons.isEmpty()) {
            return;
        }

        List<LessonDTO> oddLessons = new ArrayList<>();
        List<LessonDTO> evenLessons = new ArrayList<>();
        List<LessonDTO> weeklyLessons = new ArrayList<>();
        for (LessonDTO lesson : slotLessons) {
            Periodicity periodicity = effectivePeriodicity(lesson, timeslotsById);
            if (periodicity == Periodicity.ODD_WEEKS) {
                oddLessons.add(lesson);
            } else if (periodicity == Periodicity.EVEN_WEEKS) {
                evenLessons.add(lesson);
            } else {
                weeklyLessons.add(lesson);
            }
        }

        mergePairedLessons(oddLessons, evenLessons, weeklyLessons);

        boolean hasSplitSubgroups = slotLessons.size() > 1 && slotLessons.stream().anyMatch(lesson -> lesson.subgroup() > 0);
        boolean fallbackSplit = oddLessons.isEmpty() && evenLessons.isEmpty() && weeklyLessons.size() > 1 && !hasSplitSubgroups;
        if (fallbackSplit) {
            oddLessons = new ArrayList<>(List.of(weeklyLessons.get(0)));
            evenLessons = new ArrayList<>(weeklyLessons.subList(1, weeklyLessons.size()));
            weeklyLessons = new ArrayList<>();
        }

        boolean hasOdd = !oddLessons.isEmpty();
        boolean hasEven = !evenLessons.isEmpty();
        boolean hasWeekly = !weeklyLessons.isEmpty();
        boolean isDiagonal = !hasSplitSubgroups && (fallbackSplit || (!hasWeekly && (hasOdd || hasEven)));

        if (isDiagonal) {
            drawDiagonalSlot(content, fonts, oddLessons, evenLessons, x, y, width, height);
        } else {
            drawNormalSlot(content, fonts, slotLessons, x, y, width, height);
        }
    }

    private void drawNormalSlot(PDPageContentStream content,
                                PdfFonts fonts,
                                List<LessonDTO> lessons,
                                float x,
                                float y,
                                float width,
                                float height) throws IOException {
        List<List<LessonDTO>> groups = groupBySubject(lessons);
        float innerX = x + 4;
        float innerY = y + 4;
        float innerW = width - 8;
        float innerH = height - 8;

        if (groups.size() == 1) {
            drawLessonGroup(content, fonts, groups.get(0), innerX, innerY, innerW, innerH,
                    Align.CENTER, NORMAL_LESSON_FONT_SIZES, false);
            return;
        }

        float groupHeight = innerH / groups.size();
        for (int i = 0; i < groups.size(); i++) {
            float groupY = innerY + innerH - (i + 1) * groupHeight;
            if (i > 0) {
                content.setLineWidth(0.45f);
                content.moveTo(innerX, groupY + groupHeight);
                content.lineTo(innerX + innerW, groupY + groupHeight);
                content.stroke();
            }
            drawLessonGroup(content, fonts, groups.get(i), innerX, groupY, innerW, groupHeight,
                    Align.CENTER, COMPACT_LESSON_FONT_SIZES, false);
        }
    }

    private void drawDiagonalSlot(PDPageContentStream content,
                                  PdfFonts fonts,
                                  List<LessonDTO> oddLessons,
                                  List<LessonDTO> evenLessons,
                                  float x,
                                  float y,
                                  float width,
                                  float height) throws IOException {
        content.setLineWidth(0.85f);
        content.moveTo(x, y);
        content.lineTo(x + width, y + height);
        content.stroke();

        if (!oddLessons.isEmpty()) {
            drawLessonGroup(content, fonts, oddLessons, x + 4, y + height * 0.55f,
                    width * 0.48f, height * 0.38f, Align.LEFT, DIAGONAL_LESSON_FONT_SIZES, false);
        }
        if (!evenLessons.isEmpty()) {
            drawLessonGroup(content, fonts, evenLessons, x + width * 0.52f, y + 4,
                    width * 0.44f, height * 0.38f, Align.RIGHT, DIAGONAL_LESSON_FONT_SIZES, false);
        }
    }

    private void drawLessonGroup(PDPageContentStream content,
                                 PdfFonts fonts,
                                 List<LessonDTO> lessons,
                                 float x,
                                 float y,
                                 float width,
                                 float height,
                                 Align align,
                                 LessonTextSizes textSizes,
                                 boolean centerVertically) throws IOException {
        if (lessons.isEmpty()) {
            return;
        }

        LessonDTO first = lessons.get(0);
        LessonText lessonText = lessonText(first, lessons);
        TextBlock block = buildTextBlock(fonts, lessonText, width, textSizes);
        float totalHeight = block.height();
        float cursorY = centerVertically
                ? y + (height + totalHeight) / 2 - block.firstBaselineOffset()
                : y + height - block.firstBaselineOffset();

        for (TextLine line : block.lines()) {
            drawTextLine(content, line.text(), line.font(), line.fontSize(),
                    alignedX(x, width, textWidth(line.text(), line.font(), line.fontSize()), align),
                    cursorY,
                    line.underline(),
                    Align.LEFT);
            cursorY -= line.lineHeight();
            if (cursorY < y + 1) {
                break;
            }
        }
    }

    private TextBlock buildTextBlock(PdfFonts fonts, LessonText lesson, float width, LessonTextSizes textSizes) throws IOException {
        float subjectSize = textSizes.subjectSize(); // Subject name
        float teacherSize = textSizes.teacherSize(); // Teacher
        float roomSize = textSizes.roomSize(); // Room
        int subjectLines = SUBJECT_MAX_LINES;
        int teacherLines = TEACHER_MAX_LINES;
        int roomLines = ROOM_MAX_LINES;

        List<TextLine> lines = new ArrayList<>();
        for (String line : wrapText(lesson.subject().toUpperCase(UKRAINIAN), fonts.serifBold(), subjectSize, width, subjectLines)) {
            lines.add(new TextLine(line, fonts.serifBold(), subjectSize, subjectSize * 1.08f, true));
        }
        for (String line : wrapText(lesson.teachers(), fonts.serifRegular(), teacherSize, width, teacherLines)) {
            lines.add(new TextLine(line, fonts.serifRegular(), teacherSize, teacherSize * 1.08f, false));
        }
        for (String line : wrapText(lesson.room(), fonts.serifBold(), roomSize, width, roomLines)) {
            lines.add(new TextLine(line, fonts.serifBold(), roomSize, roomSize * 1.08f, false));
        }
        return new TextBlock(lines);
    }

    private void drawCenteredText(PDPageContentStream content,
                                  String text,
                                  PDType0Font font,
                                  float fontSize,
                                  float x,
                                  float y,
                                  float width,
                                  float height) throws IOException {
        String clean = cleanText(text);
        float textW = textWidth(clean, font, fontSize);
        float baselineX = x + (width - textW) / 2;
        float baselineY = y + (height - fontSize) / 2 + fontSize * 0.25f;
        drawTextLine(content, clean, font, fontSize, baselineX, baselineY, false, Align.LEFT);
    }

    private void drawTextLine(PDPageContentStream content,
                              String text,
                              PDType0Font font,
                              float fontSize,
                              float x,
                              float baselineY,
                              boolean underline,
                              Align align) throws IOException {
        String clean = cleanText(text);
        float textW = textWidth(clean, font, fontSize);
        float drawX = align == Align.RIGHT ? x - textW : x;

        content.beginText();
        content.setFont(font, fontSize);
        content.newLineAtOffset(drawX, baselineY);
        content.showText(clean);
        content.endText();

        if (underline && !clean.isBlank()) {
            content.setLineWidth(0.35f);
            content.moveTo(drawX, baselineY - 1.1f);
            content.lineTo(drawX + textW, baselineY - 1.1f);
            content.stroke();
        }
    }

    private void drawCellBorder(PDPageContentStream content, float x, float y, float width, float height, float lineWidth) throws IOException {
        content.setLineWidth(lineWidth);
        content.addRect(x, y, width, height);
        content.stroke();
    }

    private Map<SlotKey, List<LessonDTO>> groupLessonsBySlot(ScheduleGridDTO data, Map<Long, TimeslotDTO> timeslotsById) {
        Map<SlotKey, List<LessonDTO>> result = new LinkedHashMap<>();
        List<LessonDTO> lessons = data.lessons() == null ? List.of() : data.lessons();
        for (LessonDTO lesson : lessons) {
            if (lesson.groupId() == null || lesson.timeslotId() == null) {
                continue;
            }
            TimeslotDTO timeslot = timeslotsById.get(lesson.timeslotId());
            if (timeslot == null || timeslot.dayOfWeek() == null || timeslot.lessonNumber() == null) {
                continue;
            }
            SlotKey key = new SlotKey(lesson.groupId(), timeslot.dayOfWeek(), timeslot.lessonNumber());
            result.computeIfAbsent(key, ignored -> new ArrayList<>()).add(lesson);
        }
        return result;
    }

    private List<GroupDTO> getExportGroups(ScheduleGridDTO data, String selectedCourse) {
        Collator collator = Collator.getInstance(UKRAINIAN);
        List<GroupDTO> groups = data.groups() == null ? List.of() : data.groups();
        Integer course = parseCourse(selectedCourse);
        return groups.stream()
                .filter(Objects::nonNull)
                .filter(group -> course == null || Objects.equals(group.course(), course))
                .sorted(Comparator.comparing(group -> cleanText(group.name()), collator))
                .toList();
    }

    private Integer parseCourse(String selectedCourse) {
        if (selectedCourse == null || selectedCourse.isBlank() || selectedCourse.equalsIgnoreCase("ALL")) {
            return null;
        }
        try {
            int course = Integer.parseInt(selectedCourse.trim());
            return course >= 1 && course <= 4 ? course : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String courseLabel(String selectedCourse) {
        Integer course = parseCourse(selectedCourse);
        return course == null ? "Усі курси" : course + " курс";
    }

    private Periodicity effectivePeriodicity(LessonDTO lesson, Map<Long, TimeslotDTO> timeslotsById) {
        TimeslotDTO timeslot = lesson.timeslotId() == null ? null : timeslotsById.get(lesson.timeslotId());
        if (timeslot != null && timeslot.weekParity() != null && timeslot.weekParity() != Periodicity.WEEKLY) {
            return timeslot.weekParity();
        }
        return lesson.periodicity() == null ? Periodicity.WEEKLY : lesson.periodicity();
    }

    private void mergePairedLessons(List<LessonDTO> oddLessons, List<LessonDTO> evenLessons, List<LessonDTO> weeklyLessons) {
        for (int i = oddLessons.size() - 1; i >= 0; i--) {
            LessonDTO odd = oddLessons.get(i);
            int evenIndex = indexOfSameSubject(evenLessons, odd.subjectName());
            if (evenIndex < 0) {
                continue;
            }
            weeklyLessons.add(odd);
            weeklyLessons.add(evenLessons.remove(evenIndex));
            oddLessons.remove(i);
        }
    }

    private int indexOfSameSubject(List<LessonDTO> lessons, String subjectName) {
        for (int i = 0; i < lessons.size(); i++) {
            if (Objects.equals(cleanText(lessons.get(i).subjectName()), cleanText(subjectName))) {
                return i;
            }
        }
        return -1;
    }

    private List<List<LessonDTO>> groupBySubject(List<LessonDTO> lessons) {
        Map<String, List<LessonDTO>> grouped = new LinkedHashMap<>();
        for (LessonDTO lesson : lessons) {
            grouped.computeIfAbsent(emptyDash(lesson.subjectName()), ignored -> new ArrayList<>()).add(lesson);
        }
        return new ArrayList<>(grouped.values());
    }

    private LessonText lessonText(LessonDTO first, List<LessonDTO> lessons) {
        String subject = emptyDash(first.subjectName());
        String teachers = uniqueText(lessons.stream()
                .map(lesson -> emptyDash(lesson.teacherName()) + (lesson.teacherArchived() ? " (арх.)" : ""))
                .toList());
        String rooms = uniqueText(lessons.stream().map(lesson -> emptyDash(lesson.roomName())).toList());
        String subgroups = uniqueOptionalText(lessons.stream()
                .filter(lesson -> lesson.subgroup() > 0)
                .map(lesson -> lesson.subgroup() + "-а підгр.")
                .toList());
        String room = "ауд. №" + rooms + (subgroups.isBlank() ? "" : " " + subgroups);
        return new LessonText(subject, teachers, room);
    }

    private String uniqueText(List<String> values) {
        String unique = uniqueOptionalText(values);
        return unique.isEmpty() ? "—" : unique;
    }

    private String uniqueOptionalText(List<String> values) {
        Set<String> unique = new LinkedHashSet<>();
        for (String value : values) {
            String clean = cleanText(value);
            if (!clean.isBlank()) {
                unique.add(clean);
            }
        }
        return String.join(", ", unique);
    }

    private List<String> wrapText(String text, PDType0Font font, float fontSize, float maxWidth, int maxLines) throws IOException {
        String clean = cleanText(text);
        if (clean.isBlank()) {
            return List.of();
        }

        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : clean.split("\\s+")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (textWidth(candidate, font, fontSize) <= maxWidth) {
                current.setLength(0);
                current.append(candidate);
                continue;
            }
            if (!current.isEmpty()) {
                lines.add(current.toString());
                current.setLength(0);
            }
            current.append(word);
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        if (lines.size() <= maxLines) {
            return lines;
        }
        return new ArrayList<>(lines.subList(0, maxLines));
    }

    private float alignedX(float x, float width, float textWidth, Align align) {
        return switch (align) {
            case CENTER -> x + (width - textWidth) / 2;
            case RIGHT -> x + width - textWidth;
            case LEFT -> x;
        };
    }

    private float textWidth(String text, PDType0Font font, float fontSize) throws IOException {
        return font.getStringWidth(cleanText(text)) / 1000f * fontSize;
    }

    private PdfFonts loadFonts(PDDocument document) throws IOException {
        PDType0Font serifRegular = loadFont(document, List.of(
                "C:\\Windows\\Fonts\\times.ttf",
                "/usr/share/fonts/TTF/DejaVuSerif.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSerif.ttf",
                "/usr/share/fonts/dejavu/DejaVuSerif.ttf",
                "/usr/share/fonts/truetype/liberation/LiberationSerif-Regular.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
        ));
        PDType0Font serifBold = tryLoadFont(document, List.of(
                "C:\\Windows\\Fonts\\timesbd.ttf",
                "/usr/share/fonts/TTF/DejaVuSerif-Bold.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf",
                "/usr/share/fonts/dejavu/DejaVuSerif-Bold.ttf",
                "/usr/share/fonts/truetype/liberation/LiberationSerif-Bold.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
        ), serifRegular);
        PDType0Font sansRegular = tryLoadFont(document, List.of(
                "C:\\Windows\\Fonts\\arial.ttf",
                "/usr/share/fonts/TTF/DejaVuSans.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                "/usr/share/fonts/dejavu/DejaVuSans.ttf",
                "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf"
        ), serifRegular);
        PDType0Font sansBold = tryLoadFont(document, List.of(
                "C:\\Windows\\Fonts\\arialbd.ttf",
                "/usr/share/fonts/TTF/DejaVuSans-Bold.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
                "/usr/share/fonts/dejavu/DejaVuSans-Bold.ttf",
                "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf"
        ), sansRegular);
        return new PdfFonts(serifRegular, serifBold, sansRegular, sansBold);
    }

    private PDRectangle landscapeA4() {
        return new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
    }

    private PDType0Font loadFont(PDDocument document, List<String> candidates) throws IOException {
        for (String candidate : candidates) {
            Path path = Path.of(candidate);
            if (Files.isRegularFile(path)) {
                try (InputStream inputStream = Files.newInputStream(path)) {
                    return PDType0Font.load(document, inputStream, true);
                }
            }
        }
        throw new IOException("No TrueType font found for PDF export");
    }

    private PDType0Font tryLoadFont(PDDocument document, List<String> candidates, PDType0Font fallback) {
        try {
            return loadFont(document, candidates);
        } catch (IOException ignored) {
            return fallback;
        }
    }

    private String emptyDash(String value) {
        String clean = cleanText(value);
        return clean.isBlank() ? "—" : clean;
    }

    private String cleanText(String value) {
        return value == null ? "" : value.replace('\n', ' ').replace('\r', ' ').trim();
    }

    private record PageMetrics(PDPage page) {
        float pageWidth() {
            return page.getMediaBox().getWidth();
        }

        float pageHeight() {
            return page.getMediaBox().getHeight();
        }

        float margin() {
            return 24;
        }
    }

    private record PdfFonts(
            PDType0Font serifRegular,
            PDType0Font serifBold,
            PDType0Font sansRegular,
            PDType0Font sansBold) {
    }

    private record SlotKey(Long groupId, DayOfWeek day, Integer lessonNumber) {
    }

    private record LessonText(String subject, String teachers, String room) {
    }

    private record LessonTextSizes(float subjectSize, float teacherSize, float roomSize) {
    }

    private record TextLine(String text, PDType0Font font, float fontSize, float lineHeight, boolean underline) {
    }

    private record TextBlock(List<TextLine> lines) {
        float height() {
            return lines.stream().map(TextLine::lineHeight).reduce(0f, Float::sum);
        }

        float firstBaselineOffset() {
            return lines.isEmpty() ? 0 : lines.get(0).fontSize();
        }
    }

    private enum Align {
        LEFT,
        CENTER,
        RIGHT
    }
}
