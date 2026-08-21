import { act, fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { toast } from "sonner";
import ImportCsvModal from "./ImportCsvModal";

const mutate = vi.fn();
const reset = vi.fn();
let isPending = false;

vi.mock("../hooks", () => ({
  useImportCsv: () => ({ mutate, isPending, reset }),
}));
vi.mock("sonner", () => ({
  toast: { success: vi.fn(), error: vi.fn(), warning: vi.fn() },
}));

describe("ImportCsvModal", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    isPending = false;
  });

  it("không hiển thị gì khi isOpen=false", () => {
    render(<ImportCsvModal isOpen={false} onClose={vi.fn()} />);
    expect(screen.queryByText("Import CSV Data")).not.toBeInTheDocument();
  });

  it("báo lỗi khi bấm Import mà chưa chọn file", () => {
    render(<ImportCsvModal isOpen onClose={vi.fn()} />);
    fireEvent.click(screen.getByRole("button", { name: "Import" }));

    expect(toast.error).toHaveBeenCalledWith("Vui lòng chọn file CSV cần import");
    expect(mutate).not.toHaveBeenCalled();
  });

  it("gửi đúng entity/file đã chọn khi submit", () => {
    render(<ImportCsvModal isOpen onClose={vi.fn()} />);
    const file = new File(["name,description,type\nA,B,EXPENSE"], "category.csv", {
      type: "text/csv",
    });
    fireEvent.change(screen.getByLabelText("Loại dữ liệu"), { target: { value: "budget" } });
    fireEvent.change(screen.getByLabelText("File CSV"), { target: { files: [file] } });
    fireEvent.click(screen.getByRole("button", { name: "Import" }));

    expect(mutate).toHaveBeenCalledWith(
      { entity: "budget", file, onProgress: expect.any(Function) },
      expect.any(Object)
    );
  });

  it("hiển thị kết quả và toast khi import có dòng lỗi", () => {
    render(<ImportCsvModal isOpen onClose={vi.fn()} />);
    const file = new File(["x"], "x.csv", { type: "text/csv" });
    fireEvent.change(screen.getByLabelText("File CSV"), { target: { files: [file] } });
    fireEvent.click(screen.getByRole("button", { name: "Import" }));

    const options = mutate.mock.calls[0][1];
    act(() => {
      options.onSuccess({ successCount: 1, failedCount: 1, errors: ["Dòng 3: type không hợp lệ"] });
    });

    expect(toast.warning).toHaveBeenCalledWith("Import xong: 1 thành công, 1 lỗi");
    expect(screen.getByText("1 thành công, 1 lỗi")).toBeInTheDocument();
    expect(screen.getByText("Dòng 3: type không hợp lệ")).toBeInTheDocument();
  });

  it("đóng modal reset lại state và gọi importCsv.reset", () => {
    const onClose = vi.fn();
    render(<ImportCsvModal isOpen onClose={onClose} />);
    fireEvent.click(screen.getByRole("button", { name: "Đóng" }));

    expect(reset).toHaveBeenCalled();
    expect(onClose).toHaveBeenCalled();
  });
});
