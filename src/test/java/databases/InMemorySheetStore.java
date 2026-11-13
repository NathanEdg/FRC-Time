package databases;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemorySheetStore implements SheetStore {
  public final Map<Integer, List<List<Object>>> sheets = new HashMap<>();
  public final List<List<Cell>> writes = new ArrayList<>();
  public boolean failWrites;

  @Override
  public List<List<Object>> read(int page) {
    List<List<Object>> copy = new ArrayList<>();
    for (List<Object> row : sheets.getOrDefault(page, List.of())) {
      copy.add(new ArrayList<>(row));
    }
    return copy;
  }

  @Override
  public void write(List<Cell> cells) {
    if (cells.isEmpty()) {
      return;
    }
    if (failWrites) {
      throw new IllegalStateException("Simulated save failure.");
    }
    writes.add(List.copyOf(cells));
    for (Cell cell : cells) {
      List<List<Object>> rows = sheets.computeIfAbsent(cell.page(), ignored -> new ArrayList<>());
      while (rows.size() <= cell.row()) {
        rows.add(new ArrayList<>());
      }
      List<Object> row = rows.get(cell.row());
      while (row.size() <= cell.column()) {
        row.add("");
      }
      row.set(cell.column(), cell.value());
    }
  }
}
