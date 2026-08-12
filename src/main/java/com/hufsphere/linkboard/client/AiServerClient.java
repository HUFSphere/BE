package com.hufsphere.linkboard.client;

import com.hufsphere.linkboard.client.dto.AskRequest;
import com.hufsphere.linkboard.client.dto.AskResponse;
import com.hufsphere.linkboard.client.dto.FigmaComment;
import com.hufsphere.linkboard.client.dto.FigmaIngestRequest;
import com.hufsphere.linkboard.client.dto.FigmaIngestResponse;
import com.hufsphere.linkboard.client.dto.GithubIngestRequest;
import com.hufsphere.linkboard.client.dto.GithubIngestResponse;
import com.hufsphere.linkboard.client.dto.NotionIngestRequest;
import com.hufsphere.linkboard.client.dto.NotionIngestResponse;
import com.hufsphere.linkboard.client.dto.NotionPage;
import com.hufsphere.linkboard.exception.SourceFetchFailedException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class AiServerClient {

    private final RestClient aiServerRestClient;

    public GithubIngestResponse ingestGithub(String repo, int months) {
        try {
            return aiServerRestClient.post()
                    .uri("/ingest/github")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new GithubIngestRequest(repo, months))
                    .retrieve()
                    .body(GithubIngestResponse.class);
        } catch (RestClientException e) {
            throw new SourceFetchFailedException("외부 소스 조회에 실패했습니다");
        }
    }

    public NotionIngestResponse ingestNotion(List<NotionPage> pages) {
        try {
            return aiServerRestClient.post()
                    .uri("/ingest/notion")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new NotionIngestRequest(pages))
                    .retrieve()
                    .body(NotionIngestResponse.class);
        } catch (RestClientException e) {
            throw new SourceFetchFailedException("외부 소스 조회에 실패했습니다");
        }
    }

    public FigmaIngestResponse ingestFigma(List<FigmaComment> comments) {
        try {
            return aiServerRestClient.post()
                    .uri("/ingest/figma")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new FigmaIngestRequest(comments))
                    .retrieve()
                    .body(FigmaIngestResponse.class);
        } catch (RestClientException e) {
            throw new SourceFetchFailedException("외부 소스 조회에 실패했습니다");
        }
    }

    public AskResponse ask(String question, String lang) {
        try {
            return aiServerRestClient.post()
                    .uri("/ask")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new AskRequest(question, lang))
                    .retrieve()
                    .body(AskResponse.class);
        } catch (RestClientException e) {
            throw new SourceFetchFailedException("AI 서버 호출에 실패했습니다");
        }
    }
}
