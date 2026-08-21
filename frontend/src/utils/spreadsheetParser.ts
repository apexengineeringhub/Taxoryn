import * as XLSX from 'xlsx';

/**
 * Parses CSV, TXT, XLSX, or XLS files into a clean 2D string matrix: string[][]
 * Handles comma splitting, quotes in CSV, and native binary sheet decoding for XLSX/XLS.
 */
export async function parseSpreadsheetToRows(file: File): Promise<string[][]> {
  const fileName = file.name.toLowerCase();
  const isExcel = fileName.endsWith('.xlsx') || fileName.endsWith('.xls');

  if (isExcel) {
    const buffer = await file.arrayBuffer();
    const workbook = XLSX.read(buffer, { type: 'array', cellDates: true });
    const firstSheetName = workbook.SheetNames[0];
    const worksheet = workbook.Sheets[firstSheetName];

    // Convert sheet to array of arrays of strings
    const rows = XLSX.utils.sheet_to_json<any[]>(worksheet, {
      header: 1,
      raw: false,
      dateNF: 'yyyy-mm-dd',
      defval: '',
    });

    return rows
      .filter((row) => Array.isArray(row) && row.some((cell) => cell !== null && cell !== undefined && String(cell).trim() !== ''))
      .map((row) => row.map((cell) => (cell !== null && cell !== undefined ? String(cell).trim() : '')));
  } else {
    // CSV / TXT Parsing
    const text = await file.text();
    const lines = text.split(/\r\n|\n/).filter((l) => l.trim().length > 0);

    return lines.map((line) => {
      const row: string[] = [];
      let inQuotes = false;
      let currentCell = '';

      for (let i = 0; i < line.length; i++) {
        const char = line[i];
        if (char === '"' || char === "'") {
          inQuotes = !inQuotes;
        } else if (char === ',' && !inQuotes) {
          row.push(currentCell.trim().replace(/^["']|["']$/g, ''));
          currentCell = '';
        } else {
          currentCell += char;
        }
      }
      row.push(currentCell.trim().replace(/^["']|["']$/g, ''));
      return row;
    });
  }
}
