import apiClient from "@/lib/axios";
import type { DataEntityType, ImportResultResponse } from "./types";

const BASE = "/admin";

export const dataIoApi = {
  exportCsv: (entity: DataEntityType) =>
    apiClient.get<Blob>(`${BASE}/export/${entity}`, { responseType: "blob" }),

  importCsv: (entity: DataEntityType, file: File, onUploadProgress?: (percent: number) => void) => {
    const formData = new FormData();
    formData.append("file", file);
    return apiClient.post<ImportResultResponse>(`${BASE}/import/${entity}`, formData, {
      headers: { "Content-Type": "multipart/form-data" },
      onUploadProgress: (event) => {
        if (onUploadProgress && event.total) {
          onUploadProgress(Math.round((event.loaded / event.total) * 100));
        }
      },
    });
  },
};
