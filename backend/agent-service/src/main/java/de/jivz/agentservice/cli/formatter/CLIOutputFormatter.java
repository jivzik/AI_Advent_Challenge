package de.jivz.agentservice.cli.formatter;

import de.jivz.agentservice.cli.domain.CommandResult;
import de.jivz.agentservice.cli.domain.ContainerStatus;
import de.jivz.agentservice.cli.domain.DeploymentInfo;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.fusesource.jansi.Ansi.ansi;

/**
 * Formats CLI output with colors and emojis.
 * Single Responsibility: Output formatting only.
 */
@Component
public class CLIOutputFormatter {

    public CLIOutputFormatter() {
        AnsiConsole.systemInstall();
    }

    public void printWelcome() {
        System.out.println(ansi().fgBrightCyan().a("""
            
            ╔═══════════════════════════════════════════╗
            ║   🤖 AI DevOps Agent CLI v1.0.0          ║
            ║   Type 'help' for available commands     ║
            ╚═══════════════════════════════════════════╝
            """).reset());
    }

    public void printPrompt() {
        System.out.print(ansi().fgBrightGreen().a("\n> ").reset());
    }

    public void printSuccess(String message) {
        System.out.println(ansi().fgBrightGreen().a("✅ " + message).reset());
    }

    public void printInfo(String message) {
        System.out.println(ansi().fgBrightCyan().a("ℹ️  " + message).reset());
    }

    public void printWarning(String message) {
        System.out.println(ansi().fgBrightYellow().a("⚠️  " + message).reset());
    }

    public void printError(String message) {
        System.out.println(ansi().fgBrightRed().a("❌ " + message).reset());
    }

    public void printProgress(String message) {
        System.out.println(ansi().fgBrightBlue().a("🔄 " + message).reset());
    }

    public void printResult(CommandResult result) {
        if (result.isSuccess()) {
            printSuccess(result.getMessage());
            if (result.getDetails() != null) {
                System.out.println(result.getDetails());
            }
        } else {
            printError(result.getMessage());
            if (result.getError() != null) {
                System.out.println(ansi().fgRed().a("   Details: " + result.getError().getMessage()).reset());
            }
        }
    }

    public void printContainerStatus(List<ContainerStatus> containers) {
        System.out.println(ansi().fgBrightCyan().a("\n📊 Services Status:").reset());
        System.out.println("─".repeat(70));

        for (ContainerStatus container : containers) {
            String emoji = container.getStatusEmoji();
            String name = String.format("%-25s", container.getName());
            String status = String.format("%-12s", container.getStatus());
            String uptime = container.getUptime() != null ? container.getUptime() : "N/A";

            Ansi.Color color = container.isHealthy() ? Ansi.Color.GREEN : Ansi.Color.RED;

            System.out.println(ansi()
                .a(emoji + " ")
                .fg(color).a(name).reset()
                .a(" │ ")
                .a(status)
                .a(" │ uptime: ")
                .a(uptime)
                .reset());

            if (container.getMemoryUsage() != null) {
                System.out.println(ansi()
                    .fgBrightBlack()
                    .a("   └─ Memory: " + container.getMemoryUsage())
                    .reset());
            }
        }
        System.out.println("─".repeat(70));
    }

    public void printDeploymentProgress(DeploymentInfo deployment) {
        System.out.println(ansi()
            .fgBrightCyan()
            .a(String.format("\n🚀 Deploying %s...", deployment.getServiceName()))
            .reset());

        if (deployment.getCurrentStep() != null) {
            System.out.println(ansi()
                .fgBrightBlue()
                .a(String.format("   Step %d/%d: %s [%s]",
                    deployment.getCompletedSteps(),
                    deployment.getTotalSteps(),
                    deployment.getCurrentStep(),
                    deployment.getProgressPercentage()))
                .reset());
        }
    }

    public void printHelp() {
        System.out.println(ansi().fgBrightCyan().a("""
            
            📚 Available Commands:
            
            🚀 Deployment:
              deploy <service>        Deploy a specific service
              deploy all             Deploy all services
              rollback <service>     Rollback to previous version
            
            📊 Monitoring:
              status                 Show all container statuses
              logs <service>         Show last 20 log lines
              health <service>       Check service health
            
            📝 Release Management:
              release notes          Generate AI release notes
              create release         Create GitHub release
              commits                Show recent commits
            
            🔧 Git Operations:
              git status             Show modified/staged files
              commit "message"       Commit changes with message
              commit message "text"  Alternative commit syntax
              push                   Push to origin
              push <branch>          Push specific branch
            
            🛠️  Utility:
              help                   Show this help
              exit / quit            Exit the agent
            
            💡 Examples:
              > deploy team-service
              > показать статус
              > git status
              > изменения
              > commit "feat: Add new CLI feature"
              > push
              > push master
              > задеплой support-service
              > generate release notes
            """).reset());
    }

    public void printGoodbye() {
        System.out.println(ansi()
            .fgBrightMagenta()
            .a("\n👋 Goodbye! DevOps Agent shutting down...\n")
            .reset());
    }

    public void cleanup() {
        AnsiConsole.systemUninstall();
    }
}

