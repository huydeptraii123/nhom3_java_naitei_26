import { useMutation } from "@tanstack/react-query";
import { dataIoApi } from "./api";
import type { DataEntityType } from "./types";

export function useExportCsv() {
  return useMutation({
    mutationFn: async (entity: DataEntityType) => {
      const response = await dataIoApi.exportCsv(entity);
      return { blob: response.data, entity };
    },
  });
}

interface ImportCsvInput {
  entity: DataEntityType;
  file: File;
  onProgress?: (percent: number) => void;
}

export function useImportCsv() {
  return useMutation({
    mutationFn: ({ entity, file, onProgress }: ImportCsvInput) =>
      dataIoApi.importCsv(entity, file, onProgress).then((res) => res.data),
  });
}
