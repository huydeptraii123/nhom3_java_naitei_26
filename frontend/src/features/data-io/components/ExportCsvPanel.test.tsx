import { fireEvent, render, screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { toast } from "sonner";
import ExportCsvPanel from "./ExportCsvPanel";

const mutate = vi.fn();
const { triggerBlobDownload } = vi.hoisted(() => ({ triggerBlobDownload: vi.fn() }));

vi.mock("../hooks", () => ({
  useExportCsv: () => ({ mutate, isPending: false }),
}));
vi.mock("../downloadFile", () => ({ triggerBlobDownload }));
vi.mock("sonner", () => ({ toast: { success: vi.fn(), error: vi.fn() } }));

describe("ExportCsvPanel", () => {
  beforeEach(() => vi.clearAllMocks());

  it("hiển thị đủ nút export cho từng loại dữ liệu", () => {
    render(<ExportCsvPanel />);
    ["Người dùng", "Chi tiêu", "Thu nhập", "Danh mục", "Ngân sách"].forEach((label) => {
      expect(screen.getByText(label)).toBeInTheDocument();
    });
  });

  it("gọi export đúng entity và tải file khi thành công", () => {
    render(<ExportCsvPanel />);
    const row = screen.getByText("Danh mục").closest("div") as HTMLElement;
    fireEvent.click(within(row).getByRole("button", { name: "Export CSV" }));

    expect(mutate).toHaveBeenCalledWith("category", expect.any(Object));

    const blob = new Blob(["a"]);
    const options = mutate.mock.calls[0][1];
    options.onSuccess({ blob, entity: "category" });

    expect(triggerBlobDownload).toHaveBeenCalledWith(blob, "category.csv");
    expect(toast.success).toHaveBeenCalledWith("Xuất file CSV Danh mục thành công");
  });

  it("hiển thị lỗi qua toast khi export thất bại", () => {
    render(<ExportCsvPanel />);
    const row = screen.getByText("Ngân sách").closest("div") as HTMLElement;
    fireEvent.click(within(row).getByRole("button", { name: "Export CSV" }));

    const options = mutate.mock.calls[0][1];
    options.onError({ message: "Lỗi server" });

    expect(toast.error).toHaveBeenCalledWith("Lỗi server");
  });
});
