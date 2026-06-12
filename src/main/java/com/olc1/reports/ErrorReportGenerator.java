package com.olc1.reports;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Genera un reporte HTML estilizado con todos los errores recolectados
 * durante el proceso de compilación/interpretación.
 */
public class ErrorReportGenerator {

    /**
     * Genera el HTML, lo guarda en un archivo temporal y retorna la ruta.
     *
     * @param lexicErrors   lista de errores léxicos
     * @param syntaxErrors  lista de errores sintácticos
     * @param semanticErrors lista de errores semánticos
     * @return File apuntando al HTML generado
     */
    public static File generate(List<GoLiteError> lexicErrors,
                                List<GoLiteError> syntaxErrors,
                                List<GoLiteError> semanticErrors) throws IOException {

        File htmlFile = File.createTempFile("golite_errors_", ".html");
        htmlFile.deleteOnExit();

        try (FileWriter writer = new FileWriter(htmlFile)) {
            writer.write(buildHtml(lexicErrors, syntaxErrors, semanticErrors));
        }

        return htmlFile;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HTML builder
    // ─────────────────────────────────────────────────────────────────────────

    private static String buildHtml(List<GoLiteError> lexic,
                                    List<GoLiteError> syntax,
                                    List<GoLiteError> semantic) {

        int total = lexic.size() + syntax.size() + semantic.size();
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

        StringBuilder sb = new StringBuilder();
        sb.append("""
<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
  <title>Reporte de Errores — GoLite</title>
  <style>
    @import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&family=JetBrains+Mono:wght@400;500&display=swap');

    :root {
      --bg:        #0f1117;
      --surface:   #1a1d27;
      --surface2:  #22263a;
      --border:    #2e3249;
      --accent:    #7c6af7;
      --accent2:   #a78bfa;
      --red:       #f87171;
      --red-bg:    rgba(248,113,113,.10);
      --red-border:rgba(248,113,113,.30);
      --amber:     #fbbf24;
      --amber-bg:  rgba(251,191,36,.08);
      --amber-border:rgba(251,191,36,.28);
      --blue:      #60a5fa;
      --blue-bg:   rgba(96,165,250,.08);
      --blue-border:rgba(96,165,250,.28);
      --green:     #34d399;
      --text:      #e2e8f0;
      --text-muted:#8892a4;
      --radius:    12px;
    }

    *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }

    body {
      font-family: 'Inter', system-ui, sans-serif;
      background: var(--bg);
      color: var(--text);
      min-height: 100vh;
      padding: 2rem 1rem 4rem;
    }

    /* ── Header ── */
    .header {
      max-width: 960px;
      margin: 0 auto 2.5rem;
      display: flex;
      align-items: center;
      gap: 1.2rem;
      padding-bottom: 1.5rem;
      border-bottom: 1px solid var(--border);
    }
    .logo {
      width: 52px; height: 52px;
      border-radius: 14px;
      background: linear-gradient(135deg,#7c6af7,#a78bfa);
      display: grid; place-items: center;
      font-size: 1.6rem; font-weight: 700; color: #fff;
      flex-shrink: 0;
      box-shadow: 0 4px 20px rgba(124,106,247,.35);
    }
    .header-text h1 { font-size: 1.5rem; font-weight: 700; letter-spacing: -.02em; }
    .header-text p  { font-size: .85rem; color: var(--text-muted); margin-top: .25rem; }

    /* ── Stats bar ── */
    .stats {
      max-width: 960px;
      margin: 0 auto 2rem;
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(180px,1fr));
      gap: 1rem;
    }
    .stat-card {
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: var(--radius);
      padding: 1rem 1.25rem;
      display: flex; align-items: center; gap: .9rem;
      transition: transform .15s, box-shadow .15s;
    }
    .stat-card:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(0,0,0,.3); }
    .stat-icon {
      width: 40px; height: 40px; border-radius: 10px;
      display: grid; place-items: center; font-size: 1.1rem; flex-shrink: 0;
    }
    .stat-icon.total  { background: rgba(124,106,247,.18); }
    .stat-icon.lexic  { background: var(--red-bg);   border: 1px solid var(--red-border); }
    .stat-icon.syntax { background: var(--amber-bg); border: 1px solid var(--amber-border); }
    .stat-icon.sem    { background: var(--blue-bg);  border: 1px solid var(--blue-border); }
    .stat-label { font-size: .78rem; color: var(--text-muted); margin-bottom: .2rem; }
    .stat-value { font-size: 1.4rem; font-weight: 700; font-family: 'JetBrains Mono', monospace; }
    .stat-icon.total  + div .stat-value { color: var(--accent2); }
    .stat-icon.lexic  + div .stat-value { color: var(--red); }
    .stat-icon.syntax + div .stat-value { color: var(--amber); }
    .stat-icon.sem    + div .stat-value { color: var(--blue); }

    /* ── Section ── */
    .section {
      max-width: 960px;
      margin: 0 auto 2rem;
    }
    .section-header {
      display: flex; align-items: center; gap: .7rem;
      margin-bottom: 1rem;
    }
    .section-dot {
      width: 10px; height: 10px; border-radius: 50%;
    }
    .section-dot.red    { background: var(--red); box-shadow: 0 0 8px var(--red); }
    .section-dot.amber  { background: var(--amber); box-shadow: 0 0 8px var(--amber); }
    .section-dot.blue   { background: var(--blue); box-shadow: 0 0 8px var(--blue); }
    .section-title {
      font-size: 1rem; font-weight: 600; letter-spacing: -.01em;
    }
    .badge {
      margin-left: auto;
      font-size: .72rem; font-weight: 600;
      padding: .2rem .6rem;
      border-radius: 999px;
    }
    .badge.red    { background: var(--red-bg);   color: var(--red);   border: 1px solid var(--red-border); }
    .badge.amber  { background: var(--amber-bg); color: var(--amber); border: 1px solid var(--amber-border); }
    .badge.blue   { background: var(--blue-bg);  color: var(--blue);  border: 1px solid var(--blue-border); }

    /* ── Table ── */
    .table-wrap {
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: var(--radius);
      overflow: hidden;
    }
    table { width: 100%; border-collapse: collapse; }
    thead { background: var(--surface2); }
    th {
      padding: .65rem 1rem;
      text-align: left;
      font-size: .75rem;
      font-weight: 600;
      color: var(--text-muted);
      letter-spacing: .06em;
      text-transform: uppercase;
      border-bottom: 1px solid var(--border);
    }
    td {
      padding: .75rem 1rem;
      font-size: .85rem;
      border-bottom: 1px solid var(--border);
      vertical-align: top;
    }
    tr:last-child td { border-bottom: none; }
    tr:hover td { background: var(--surface2); }

    .type-chip {
      display: inline-block;
      padding: .2rem .55rem;
      border-radius: 6px;
      font-size: .75rem;
      font-weight: 600;
      font-family: 'JetBrains Mono', monospace;
    }
    .type-chip.lexico   { background: var(--red-bg);   color: var(--red);   border: 1px solid var(--red-border); }
    .type-chip.sintaxis { background: var(--amber-bg); color: var(--amber); border: 1px solid var(--amber-border); }
    .type-chip.semantico{ background: var(--blue-bg);  color: var(--blue);  border: 1px solid var(--blue-border); }

    .location-code {
      font-family: 'JetBrains Mono', monospace;
      font-size: .8rem;
      color: var(--text-muted);
      white-space: nowrap;
    }
    .location-code span { color: var(--accent2); }

    .desc-text { color: var(--text); line-height: 1.5; }

    /* ── Empty state ── */
    .empty {
      text-align: center;
      padding: 2.5rem;
      color: var(--text-muted);
    }
    .empty-icon { font-size: 2rem; margin-bottom: .5rem; }
    .empty p    { font-size: .9rem; }

    /* ── Success banner ── */
    .success-banner {
      max-width: 960px;
      margin: 0 auto 2rem;
      background: rgba(52,211,153,.08);
      border: 1px solid rgba(52,211,153,.25);
      border-radius: var(--radius);
      padding: 1.2rem 1.5rem;
      display: flex; align-items: center; gap: 1rem;
    }
    .success-banner .icon { font-size: 1.8rem; }
    .success-banner strong { color: var(--green); font-size: 1rem; }
    .success-banner p { font-size: .85rem; color: var(--text-muted); margin-top: .15rem; }

    /* ── Footer ── */
    footer {
      max-width: 960px;
      margin: 3rem auto 0;
      padding-top: 1.5rem;
      border-top: 1px solid var(--border);
      font-size: .8rem;
      color: var(--text-muted);
      display: flex; justify-content: space-between; align-items: center;
      flex-wrap: wrap; gap: .5rem;
    }
    footer strong { color: var(--accent2); }
  </style>
</head>
<body>
""");

        // ── Header
        sb.append("""
  <div class="header">
    <div class="logo">GL</div>
    <div class="header-text">
      <h1>Reporte de Errores</h1>
      <p>GoLite &mdash; Laboratorio OLC1 &nbsp;|&nbsp; Generado el """ + timestamp + """
</p>
    </div>
  </div>
""");

        // ── Stats
        sb.append("""
  <div class="stats">
    <div class="stat-card">
      <div class="stat-icon total">⚠️</div>
      <div><div class="stat-label">TOTAL ERRORES</div>
           <div class="stat-value">""" + total + """
</div></div>
    </div>
    <div class="stat-card">
      <div class="stat-icon lexic">🔤</div>
      <div><div class="stat-label">LÉXICOS</div>
           <div class="stat-value">""" + lexic.size() + """
</div></div>
    </div>
    <div class="stat-card">
      <div class="stat-icon syntax">📐</div>
      <div><div class="stat-label">SINTÁCTICOS</div>
           <div class="stat-value">""" + syntax.size() + """
</div></div>
    </div>
    <div class="stat-card">
      <div class="stat-icon sem">🧠</div>
      <div><div class="stat-label">SEMÁNTICOS</div>
           <div class="stat-value">""" + semantic.size() + """
</div></div>
    </div>
  </div>
""");

        // ── Success banner (si no hay errores)
        if (total == 0) {
            sb.append("""
  <div class="success-banner">
    <div class="icon">✅</div>
    <div>
      <strong>Sin errores detectados</strong>
      <p>El programa fue procesado exitosamente sin ningún error léxico, sintáctico ni semántico.</p>
    </div>
  </div>
""");
        }

        // ── Sección Léxicos
        sb.append(buildSection("Errores Léxicos",
                "red", "🔤", "lexico", lexic));

        // ── Sección Sintácticos
        sb.append(buildSection("Errores Sintácticos",
                "amber", "📐", "sintaxis", syntax));

        // ── Sección Semánticos
        sb.append(buildSection("Errores Semánticos",
                "blue", "🧠", "semantico", semantic));

        // ── Footer
        sb.append("""
  <footer>
    <span>GoLite Interpreter &mdash; <strong>OLC1</strong> &nbsp;|&nbsp; USAC</span>
    <span>Reporte generado automáticamente</span>
  </footer>
</body>
</html>
""");

        return sb.toString();
    }

    private static String buildSection(String title, String color,
                                       String icon, String chipClass,
                                       List<GoLiteError> errors) {
        StringBuilder sb = new StringBuilder();
        sb.append("  <div class=\"section\">\n");
        sb.append("    <div class=\"section-header\">\n");
        sb.append("      <div class=\"section-dot ").append(color).append("\"></div>\n");
        sb.append("      <h2 class=\"section-title\">").append(icon)
          .append(" &nbsp;").append(title).append("</h2>\n");
        sb.append("      <span class=\"badge ").append(color).append("\">")
          .append(errors.size()).append(" encontrado").append(errors.size() == 1 ? "" : "s")
          .append("</span>\n");
        sb.append("    </div>\n");
        sb.append("    <div class=\"table-wrap\">\n");

        if (errors.isEmpty()) {
            sb.append("""
      <div class="empty">
        <div class="empty-icon">✓</div>
        <p>No se encontraron errores de este tipo.</p>
      </div>
""");
        } else {
            sb.append("""
      <table>
        <thead>
          <tr>
            <th>#</th>
            <th>Tipo</th>
            <th>Ubicación</th>
            <th>Descripción</th>
          </tr>
        </thead>
        <tbody>
""");
            int idx = 1;
            for (GoLiteError err : errors) {
                sb.append("          <tr>\n");
                sb.append("            <td class=\"location-code\">").append(idx++).append("</td>\n");
                sb.append("            <td><span class=\"type-chip ").append(chipClass)
                  .append("\">").append(escapeHtml(err.getType())).append("</span></td>\n");
                sb.append("            <td class=\"location-code\">Línea <span>")
                  .append(err.getLine()).append("</span>, Col <span>")
                  .append(err.getColumn()).append("</span></td>\n");
                sb.append("            <td class=\"desc-text\">")
                  .append(escapeHtml(err.getDescription())).append("</td>\n");
                sb.append("          </tr>\n");
            }
            sb.append("        </tbody>\n      </table>\n");
        }

        sb.append("    </div>\n  </div>\n");
        return sb.toString();
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}
