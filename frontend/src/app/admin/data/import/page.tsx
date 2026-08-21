import ImportCsvSection from "@/features/data-io/components/ImportCsvSection";

export default function ImportDataPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Import CSV Data</h1>
        <p className="mt-1 text-sm text-gray-500">Nhập dữ liệu hệ thống từ file CSV</p>
      </div>
      <ImportCsvSection />
    </div>
  );
}
