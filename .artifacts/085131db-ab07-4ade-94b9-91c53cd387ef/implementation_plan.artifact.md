# Implementation Plan - Functional Note Saving and UI Enhancements

Enable functional note writing, saving, and enhance the note detail UI with a custom header and flexible text entry.

## User Review Required

> [!IMPORTANT]
> - **Auto-save Logic**: The plan implements auto-saving whenever the user edits the note.
> - **Flexible Structure**: The title and body are both editable. If the user wants a completely "no fixed title" experience, we could merge them into one field, but keeping them separate in the DB allows for better grid display. I will make the title field optional and less prominent.
> - **Three-dot Menu**: I will implement a circular menu button in the detail view that includes a "Delete" option, similar to the main screen.

## Proposed Changes

### Note Detail Screen Enhancements

#### [MODIFY] [NoteListDetailScreen.kt](file:///D:/DadesProject/app/src/main/java/com/sn00bol/dades/ui/screens/notes/NoteListDetailScreen.kt)
- Update `NoteDetailPane` to be stateful and support editing.
- Add a custom header with:
    - Back button (left).
    - Circular three-dot menu button (right).
- Use `TextField`s with minimal styling for a flexible writing experience.
- Implement auto-save logic using `LaunchedEffect` or `onValueChange` callbacks to `NoteRepository`.
- Ensure the back button correctly triggers `navigator.navigateBack()`.

## Verification Plan

### Automated Tests
- Not applicable for this UI-heavy task, but I will verify the build.

### Manual Verification
1. Open the app and click "New Note".
2. Type in the note and title.
3. Observe if the note appears in the grid after navigating back.
4. Open an existing note, edit it, and verify changes are saved.
5. Test the back button and the three-dot menu in the detail view.
