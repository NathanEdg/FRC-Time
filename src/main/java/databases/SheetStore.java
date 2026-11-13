package databases;

import java.util.List;

/** Zero-based writes to either attendance worksheet. */
public interface SheetStore {
  record Cell(int page, int row, int column, String value) {}

  List<List<Object>> read(int page);

  void write(List<Cell> cells);
}
