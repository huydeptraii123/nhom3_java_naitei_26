"use client";

import { useState } from "react";
import Button from "@/components/ui/Button";
import Card from "@/components/ui/Card";
import ImportCsvModal from "./ImportCsvModal";

export default function ImportCsvSection() {
  const [isOpen, setIsOpen] = useState(false);

  return (
    <Card
      title="Import CSV Data"
      description="Nhập dữ liệu vào hệ thống từ file CSV theo từng loại"
      action={<Button onClick={() => setIsOpen(true)}>Import CSV</Button>}
    >
      <p className="text-sm text-gray-500">
        Chọn loại dữ liệu, tải lên file CSV đúng định dạng cột yêu cầu. Dòng lỗi sẽ bị bỏ
        qua và liệt kê chi tiết, các dòng hợp lệ vẫn được nhập bình thường.
      </p>
      <ImportCsvModal isOpen={isOpen} onClose={() => setIsOpen(false)} />
    </Card>
  );
}
