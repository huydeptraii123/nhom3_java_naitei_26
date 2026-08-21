"use client";

import { useState } from "react";
import { toast } from "sonner";
import Button from "@/components/ui/Button";
import Card from "@/components/ui/Card";
import { useExportCsv } from "../hooks";
import { DATA_ENTITIES, type DataEntityType } from "../types";
import { triggerBlobDownload } from "../downloadFile";

export default function ExportCsvPanel() {
  const [exportingEntity, setExportingEntity] = useState<DataEntityType | null>(null);
  const exportCsv = useExportCsv();

  const handleExport = (entity: DataEntityType, label: string) => {
    setExportingEntity(entity);
    exportCsv.mutate(entity, {
      onSuccess: ({ blob }) => {
        triggerBlobDownload(blob, `${entity}.csv`);
        toast.success(`Xuất file CSV ${label} thành công`);
      },
      onError: (error: { message?: string }) => {
        toast.error(error.message || `Không thể xuất file CSV ${label}`);
      },
      onSettled: () => setExportingEntity(null),
    });
  };

  return (
    <Card
      title="Export CSV Data"
      description="Xuất dữ liệu hệ thống ra file CSV theo từng loại"
    >
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {DATA_ENTITIES.map(({ value, label }) => (
          <div
            key={value}
            className="flex items-center justify-between rounded-lg border border-gray-200 p-4"
          >
            <span className="text-sm font-medium text-gray-900">{label}</span>
            <Button
              size="sm"
              variant="outline"
              isLoading={exportingEntity === value}
              onClick={() => handleExport(value, label)}
            >
              Export CSV
            </Button>
          </div>
        ))}
      </div>
    </Card>
  );
}
