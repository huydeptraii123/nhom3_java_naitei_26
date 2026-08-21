export type DataEntityType = "user" | "expense" | "income" | "category" | "budget";

export interface ImportResultResponse {
  successCount: number;
  failedCount: number;
  errors: string[];
}

export const DATA_ENTITIES: { value: DataEntityType; label: string }[] = [
  { value: "user", label: "Người dùng" },
  { value: "expense", label: "Chi tiêu" },
  { value: "income", label: "Thu nhập" },
  { value: "category", label: "Danh mục" },
  { value: "budget", label: "Ngân sách" },
];
