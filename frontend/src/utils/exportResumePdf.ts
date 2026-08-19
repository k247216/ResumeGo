const A4_WIDTH_MM = 210
const A4_HEIGHT_MM = 297
const PAGE_OVERFLOW_TOLERANCE_MM = 3
const EXPORT_WIDTH_PX = 794
const EXPORT_MIN_HEIGHT_PX = 1123

export interface ExportResumePdfOptions {
  sourceElement: HTMLElement
  fileName: string
}

export async function exportResumeElementToPdf({
  sourceElement,
  fileName,
}: ExportResumePdfOptions): Promise<void> {
  const exportHost = document.createElement('div')
  const clonedPaper = sourceElement.cloneNode(true) as HTMLElement

  exportHost.className = 'resume-pdf-export-host'
  Object.assign(exportHost.style, {
    position: 'fixed',
    top: '0',
    left: '-10000px',
    zIndex: '-1',
    width: `${EXPORT_WIDTH_PX}px`,
    minHeight: `${EXPORT_MIN_HEIGHT_PX}px`,
    overflow: 'visible',
    background: '#ffffff',
    pointerEvents: 'none',
  })

  preparePaperForExport(clonedPaper)
  exportHost.appendChild(clonedPaper)
  document.body.appendChild(exportHost)

  try {
    const [{ default: html2canvas }, { jsPDF }] = await Promise.all([
      import('html2canvas'),
      import('jspdf'),
    ])

    await document.fonts?.ready
    const canvas = await html2canvas(clonedPaper, {
      backgroundColor: '#ffffff',
      scale: Math.max(2, window.devicePixelRatio || 1),
      useCORS: true,
      logging: false,
      windowWidth: EXPORT_WIDTH_PX,
      windowHeight: Math.max(EXPORT_MIN_HEIGHT_PX, clonedPaper.scrollHeight),
    })

    const imageData = canvas.toDataURL('image/png')
    const pdf = new jsPDF({
      orientation: 'portrait',
      unit: 'mm',
      format: 'a4',
      compress: true,
    })

    const imageHeightMm = (canvas.height * A4_WIDTH_MM) / canvas.width
    const pageCount = Math.max(
      1,
      Math.ceil((imageHeightMm - PAGE_OVERFLOW_TOLERANCE_MM) / A4_HEIGHT_MM),
    )

    for (let pageIndex = 0; pageIndex < pageCount; pageIndex += 1) {
      if (pageIndex > 0) {
        pdf.addPage()
      }
      const renderedHeightMm = pageIndex * A4_HEIGHT_MM
      pdf.addImage(
        imageData,
        'PNG',
        0,
        -renderedHeightMm,
        A4_WIDTH_MM,
        imageHeightMm,
        undefined,
        'FAST',
      )
    }

    pdf.save(normalizePdfFileName(fileName))
  } finally {
    exportHost.remove()
  }
}

function preparePaperForExport(paper: HTMLElement) {
  paper.querySelectorAll('.active').forEach((element) => {
    element.classList.remove('active')
  })

  Object.assign(paper.style, {
    position: 'static',
    top: 'auto',
    left: 'auto',
    width: `${EXPORT_WIDTH_PX}px`,
    minHeight: `${EXPORT_MIN_HEIGHT_PX}px`,
    margin: '0',
    boxShadow: 'none',
    transform: 'none',
    transformOrigin: 'top left',
  })
  paper.style.setProperty('--paper-scale', '1')
}

function normalizePdfFileName(fileName: string) {
  const safeName = fileName
    .trim()
    .replace(/[\\/:*?"<>|]/g, '-')
    .replace(/\s+/g, '-')
    .replace(/-+/g, '-')
    .replace(/^-|-$/g, '')

  return `${safeName || 'resume'}.pdf`
}
