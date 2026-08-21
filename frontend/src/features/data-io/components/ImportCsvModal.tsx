"use client";

import { useState } from "react";
import { toast } from "sonner";
import Button from "@/components/ui/Button";
import Modal from "@/components/ui/Modal";
import Select from "@/components/ui/Select";
import { useImportCsv } from "../hooks";
import { DATA_ENTITIES, type DataEntityType, type ImportResultResponse } from "../types";

interface ImportCsvModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export default function ImportCsvModal({ isOpen, onClose }: ImportCsvModalProps) {
  const [entity, setEntity] = useState<DataEntityType>(DATA_ENTITIES[0].value);
  const [file, setFile] = useState<File | null>(null);
  const [progress, setProgress] = useState(0);
  const [result, setResult] = useState<ImportResultResponse | null>(null);
  const importCsv = useImportCsv();

  const handleClose = () => {
    setFile(null);
    setProgress(0);
    setResult(null);
    importCsv.reset();
    onClose();
  };

  const handleSubmit = () => {
    if (!file) {
      toast.error("Vui lòng chọn file CSV cần import");
      return;
    }
    setProgress(0);
    setResult(null);
    importCsv.mutate(
      { entity, file, onProgress: setProgress },
      {
        onSuccess: (data) => {
          setResult(data);
          if (data.failedCount === 0) {
            toast.success(`Import thành công ${data.successCount} dòng`);
          } else {
            toast.warning(`Import xong: ${data.successCount} thành công, ${data.failedCount} lỗi`);
          }
        },
        onError: (error: { message?: string }) => {
          toast.error(error.message || "Import CSV thất bại");
        },
      }
    );
  };

  return (
    <Modal isOpen={isOpen} onClose={handleClose} title="Import CSV Data" size="lg">
      <div className="space-y-4">
        <Select
          label="Loại dữ liệu"
          value={entity}
          onChange={(event) => setEntity(event.target.value as DataEntityType)}
          options={DATA_ENTITIES.map(({ value, label }) => ({ value, label }))}
          disabled={importCsv.isPending}
        />

        <div>
          <label htmlFor="import-csv-file" className="mb-1 block text-sm font-medium text-gray-700">
            File CSV
          </label>
          <input
            id="import-csv-file"
            type="file"
            accept=".csv,text/csv"
            disabled={importCsv.isPending}
            className="block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm file:mr-3 file:rounded-md file:border-0 file:bg-blue-50 file:px-3 file:py-1 file:text-blue-700"
            onChange={(event) => setFile(event.target.files?.[0] ?? null)}
          />
        </div>

        {importCsv.isPending && (
          <div>
            <div className="h-2 w-full overflow-hidden rounded-full bg-gray-100">
              <div
                className="h-full bg-blue-600 transition-all duration-150"
                style={{ width: `${progress}%` }}
              />
            </div>
            <p className="mt-1 text-xs text-gray-500">Đang tải lên... {progress}%</p>
          </div>
        )}

        {result && (
          <div className="rounded-lg border border-gray-200 p-3 text-sm">
            <p className="font-medium text-gray-900">
              {result.successCount} thành công, {result.failedCount} lỗi
            </p>
            {result.errors.length > 0 && (
              <ul className="mt-2 max-h-40 list-disc space-y-1 overflow-y-auto pl-5 text-red-600">
                {result.errors.map((error, index) => (
                  <li key={index}>{error}</li>
                ))}
              </ul>
            )}
          </div>
        )}

        <div className="flex justify-end gap-2 pt-2">
          <Button variant="outline" onClick={handleClose} disabled={importCsv.isPending}>
            Đóng
          </Button>
          <Button onClick={handleSubmit} isLoading={importCsv.isPending}>
            Import
          </Button>
        </div>
      </div>
    </Modal>
  );
}
