package databases;

import helpers.Constants;
import java.util.ArrayList;
import java.util.List;

public class DatabaseUtils {
  private final SheetStore store;
  private List<List<Object>> current;
  private List<List<Object>> log;
  private CurrentSheetLayout layout;

  public DatabaseUtils() {
    this(new DatabaseProcess());
  }

  public DatabaseUtils(SheetStore store) {
    this.store = store;
    getUpdatedData();
  }

  public void getUpdatedData() {
    List<List<Object>> updatedCurrent = store.read(Constants.kMainSheet);
    List<List<Object>> updatedLog = store.read(Constants.kLogSheet);
    // Validate existing data before initializing any missing headers.
    CurrentSheetLayout updatedLayout =
        CurrentSheetLayout.fromHeaders(
            updatedCurrent.isEmpty() ? CurrentSheetLayout.HEADERS : updatedCurrent.get(0));
    if (!updatedLog.isEmpty()) {
      CurrentSheetLayout.validateLogHeaders(updatedLog.get(0));
    }
    List<SheetStore.Cell> headers = new ArrayList<>();
    if (updatedCurrent.isEmpty()) {
      for (int i = 0; i < CurrentSheetLayout.HEADERS.size(); i++) {
        headers.add(
            new SheetStore.Cell(
                Constants.kMainSheet, 0, i, String.valueOf(CurrentSheetLayout.HEADERS.get(i))));
      }
    }
    if (updatedLog.isEmpty()) {
      for (int i = 0; i < 3; i++) {
        headers.add(
            new SheetStore.Cell(
                Constants.kLogSheet, 0, i, String.valueOf(CurrentSheetLayout.HEADERS.get(i))));
      }
    }
    store.write(headers);
    layout = updatedLayout;
    current = updatedCurrent.isEmpty() ? List.of(CurrentSheetLayout.HEADERS) : updatedCurrent;
    log = updatedLog.isEmpty() ? List.of(CurrentSheetLayout.HEADERS.subList(0, 3)) : updatedLog;
  }

  public void write(List<SheetStore.Cell> cells) {
    List<SheetStore.Cell> mapped = new ArrayList<>();
    for (SheetStore.Cell cell : cells) {
      mapped.add(
          new SheetStore.Cell(
              cell.page(),
              cell.row(),
              cell.page() == Constants.kMainSheet
                  ? layout.physicalColumn(cell.column())
                  : cell.column(),
              cell.value()));
    }
    store.write(mapped);
  }

  public void setCellDataBatch(ArrayList<BatchUpdateData> data, int page) {
    List<SheetStore.Cell> cells = new ArrayList<>();
    for (BatchUpdateData item : data) {
      cells.add(new SheetStore.Cell(page, item.getRow() - 1, item.getColumn() - 1, item.getData()));
    }
    write(cells);
  }

  public ArrayList<String> getColumnData(int column, int page) {
    int mapped = page == Constants.kMainSheet ? layout.physicalColumn(column) : column;
    ArrayList<String> result = new ArrayList<>();
    for (List<Object> row : page == Constants.kMainSheet ? current : log) {
      result.add(mapped < row.size() ? String.valueOf(row.get(mapped)) : "");
    }
    return result;
  }

  public ArrayList<String> getRowData(int oneBasedRow, int page) {
    List<List<Object>> rows = page == Constants.kMainSheet ? current : log;
    ArrayList<String> result = new ArrayList<>();
    if (oneBasedRow < 1 || oneBasedRow > rows.size()) {
      return result;
    }
    for (Object cell : rows.get(oneBasedRow - 1)) {
      result.add(String.valueOf(cell));
    }
    return result;
  }

  public void setCellData(int row, int column, String value, int page) {
    write(List.of(new SheetStore.Cell(page, row, column, value)));
  }

  public String getCellData(int row, int column, int page) {
    List<String> values = getColumnData(column, page);
    return row >= 0 && row < values.size() ? values.get(row) : "";
  }

  public int getCellRowFromColumn(String value, int column, int page) {
    return getColumnData(column, page).indexOf(value);
  }

  public List<List<Object>> readMeetings() {
    return store.read(Constants.kMeetingsSheet);
  }

  public int logWidth() {
    return log.stream().mapToInt(List::size).max().orElse(0);
  }

  public int nextEmptyCellColumn(int page) {
    // A blank ID can belong to a partially cleared record; never reuse historical rows.
    return (page == Constants.kMainSheet ? current : log).size();
  }
}
