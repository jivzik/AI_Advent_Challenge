package de.jivz.supportservice.service.orchestrator;

/**
 * Thread-Local-Context für die Ticket-Erstellung während des Tool-Execution-Loops.
 * Ermöglicht das Speichern und Abrufen der ticketNumber und GitHub Issue URL über Thread-Grenzen hinweg.
 */
public class ThreadLocalTicketContext {

    private static final ThreadLocal<String> ticketNumberHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> gitHubIssueUrlHolder = new ThreadLocal<>();

    /**
     * Speichert die Ticket-Nummer im Thread-Local-Context.
     */
    public static void setTicketNumber(String ticketNumber) {
        ticketNumberHolder.set(ticketNumber);
    }

    /**
     * Ruft die gespeicherte Ticket-Nummer ab.
     */
    public static String getTicketNumber() {
        return ticketNumberHolder.get();
    }

    /**
     * Speichert die GitHub Issue URL im Thread-Local-Context.
     */
    public static void setGitHubIssueUrl(String url) {
        gitHubIssueUrlHolder.set(url);
    }

    /**
     * Ruft die gespeicherte GitHub Issue URL ab.
     */
    public static String getGitHubIssueUrl() {
        return gitHubIssueUrlHolder.get();
    }

    /**
     * Löscht den Thread-Local-Context.
     */
    public static void clear() {
        ticketNumberHolder.remove();
        gitHubIssueUrlHolder.remove();
    }
}

