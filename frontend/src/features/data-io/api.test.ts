import { beforeEach, describe, expect, it, vi } from "vitest";
import apiClient from "@/lib/axios";
import { dataIoApi } from "./api";

vi.mock("@/lib/axios", () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

describe("dataIoApi.exportCsv", () => {
  beforeEach(() => vi.clearAllMocks());

  it("gọi GET /admin/export/{entity} với responseType blob", async () => {
    const blob = new Blob(["a,b\n1,2"], { type: "text/csv" });
    vi.mocked(apiClient.get).mockResolvedValue({ data: blob });

    const result = await dataIoApi.exportCsv("category");

    expect(apiClient.get).toHaveBeenCalledWith("/admin/export/category", {
      responseType: "blob",
    });
    expect(result.data).toBe(blob);
  });
});

describe("dataIoApi.importCsv", () => {
  beforeEach(() => vi.clearAllMocks());

  it("gửi multipart form-data với field file", async () => {
    const response = { successCount: 1, failedCount: 0, errors: [] };
    vi.mocked(apiClient.post).mockResolvedValue({ data: response });
    const file = new File(["name,type\nA,EXPENSE"], "category.csv", { type: "text/csv" });

    const result = await dataIoApi.importCsv("category", file);

    expect(apiClient.post).toHaveBeenCalledWith(
      "/admin/import/category",
      expect.any(FormData),
      expect.objectContaining({ headers: { "Content-Type": "multipart/form-data" } })
    );
    const formData = vi.mocked(apiClient.post).mock.calls[0][1] as FormData;
    expect(formData.get("file")).toBe(file);
    expect(result.data).toEqual(response);
  });

  it("gọi onUploadProgress với phần trăm đã tính", async () => {
    vi.mocked(apiClient.post).mockImplementation(async (_url, _body, config) => {
      config?.onUploadProgress?.({ loaded: 50, total: 100 } as never);
      return { data: { successCount: 0, failedCount: 0, errors: [] } };
    });
    const onProgress = vi.fn();
    const file = new File(["x"], "x.csv", { type: "text/csv" });

    await dataIoApi.importCsv("expense", file, onProgress);

    expect(onProgress).toHaveBeenCalledWith(50);
  });
});
