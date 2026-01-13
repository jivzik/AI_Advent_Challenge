package de.jivz.mcp.tools.git;

import de.jivz.mcp.model.InputSchema;
import de.jivz.mcp.model.PropertyDefinition;
import de.jivz.mcp.model.ToolDefinition;
import de.jivz.mcp.tools.Tool;
import de.jivz.mcp.tools.ToolArguments;
import de.jivz.mcp.tools.ToolExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Инструмент для сравнения двух Git веток.
 * Показывает коммиты, которые есть в одной ветке, но отсутствуют в другой.
 */
@Component
@Slf4j
public class CompareBranchesTool extends GitToolBase implements Tool {

    private static final String NAME = "compare_branches";
    private static final int MAX_COMMITS = 100;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    static {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public ToolDefinition getDefinition() {
        Map<String, PropertyDefinition> properties = new LinkedHashMap<>();

        properties.put("base", PropertyDefinition.builder()
                .type("string")
                .description("Базовая ветка для сравнения (например, 'main' или 'develop')")
                .build());

        properties.put("compare", PropertyDefinition.builder()
                .type("string")
                .description("Ветка для сравнения с базовой (например, 'feature/new-feature')")
                .build());

        List<String> required = Arrays.asList("base", "compare");

        return ToolDefinition.builder()
                .name(NAME)
                .description("Сравнить две Git ветки и показать различия в коммитах")
                .inputSchema(InputSchema.builder()
                        .type("object")
                        .properties(properties)
                        .required(required)
                        .build())
                .build();
    }

    @Override
    public Object execute(Map<String, Object> arguments) {
        ToolArguments args = ToolArguments.of(arguments);
        String baseBranch = args.getRequiredString("base");
        String compareBranch = args.getRequiredString("compare");

        log.info("🔧 Выполнение {}: сравнение веток {} и {}", NAME, baseBranch, compareBranch);

        try (Git git = getGitRepository()) {
            // Получаем ссылки на ветки
            Ref baseRef = git.getRepository().findRef(baseBranch);
            Ref compareRef = git.getRepository().findRef(compareBranch);

            if (baseRef == null) {
                throw new ToolExecutionException("Базовая ветка не найдена: " + baseBranch);
            }

            if (compareRef == null) {
                throw new ToolExecutionException("Ветка для сравнения не найдена: " + compareBranch);
            }

            ObjectId baseId = baseRef.getObjectId();
            ObjectId compareId = compareRef.getObjectId();

            // Получаем коммиты, которые есть в compare, но нет в base
            List<Map<String, String>> aheadCommits = getCommitsDifference(git, compareId, baseId);

            // Получаем коммиты, которые есть в base, но нет в compare
            List<Map<String, String>> behindCommits = getCommitsDifference(git, baseId, compareId);

            log.info("✅ Ветка {} опережает {} на {} коммитов и отстает на {} коммитов",
                    compareBranch, baseBranch, aheadCommits.size(), behindCommits.size());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("base", baseBranch);
            result.put("compare", compareBranch);
            result.put("ahead", aheadCommits.size());
            result.put("behind", behindCommits.size());
            result.put("aheadCommits", aheadCommits);
            result.put("behindCommits", behindCommits);

            return result;

        } catch (ToolExecutionException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ Ошибка при сравнении веток", e);
            throw new ToolExecutionException("Ошибка при сравнении веток: " + e.getMessage());
        }
    }

    /**
     * Получить коммиты, которые есть в source, но нет в target.
     */
    private List<Map<String, String>> getCommitsDifference(Git git, ObjectId source, ObjectId target) throws Exception {
        List<Map<String, String>> commits = new ArrayList<>();

        try (RevWalk walk = new RevWalk(git.getRepository())) {
            walk.markStart(walk.parseCommit(source));
            walk.markUninteresting(walk.parseCommit(target));
            walk.setRevFilter(RevFilter.NO_MERGES);

            int count = 0;
            for (RevCommit commit : walk) {
                if (count >= MAX_COMMITS) {
                    break;
                }

                Map<String, String> commitInfo = new LinkedHashMap<>();
                commitInfo.put("hash", commit.getName());
                commitInfo.put("shortHash", commit.getName().substring(0, 7));
                commitInfo.put("author", commit.getAuthorIdent().getName());
                commitInfo.put("date", DATE_FORMAT.format(new Date(commit.getCommitTime() * 1000L)));
                commitInfo.put("message", commit.getShortMessage());

                commits.add(commitInfo);
                count++;
            }
        }

        return commits;
    }
}

