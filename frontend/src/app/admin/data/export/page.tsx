import ExportCsvPanel from "@/features/data-io/components/ExportCsvPanel";

export default function ExportDataPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Export CSV Data</h1>
        <p className="mt-1 text-sm text-gray-500">Xuất dữ liệu hệ thống ra file CSV</p>
      </div>
      <ExportCsvPanel />
    </div>
  );
}
