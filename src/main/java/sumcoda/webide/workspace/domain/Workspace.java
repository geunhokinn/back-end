package sumcoda.webide.workspace.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sumcoda.webide.chat.domain.ChatRoom;
import sumcoda.webide.entry.domain.Entry;
import sumcoda.webide.memberworkspace.domain.MemberWorkspace;

import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class Workspace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 컨테이너 생성시 어떤 컨테이너인지 설명
    @Column(nullable = false)
    private String title;

    // 해당 컨테이너가 어떤 종류의 컨테이너인지
    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String language;

    @Column(nullable = false)
    private String content;

    // private인지 public 인지
    @Column(nullable = false)
    private Boolean status;

    // 양방향 연관관계
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "workspace")
    private List<MemberWorkspace> memberWorkspaces;

    // 양방향 연관관계
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "workspace")
    private List<Entry> entries;

    // 연관관게 주인
    // 양방향 연관관계
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;



    @Builder
    public Workspace(String title, String category, String language, String content, Boolean status) {
        this.title = title;
        this.category = category;
        this.language = language;
        this.content = content;
        this.status = status;
    }

    // Workspace 1 <-> N MemberWorkspace
    // 양방향 연관관계 편의 메서드
    public void addMemberWorkspace(MemberWorkspace memberWorkspace) {
        this.memberWorkspaces.add(memberWorkspace);

        if (memberWorkspace.getWorkspace() != this) {
            memberWorkspace.assignWorkspace(this);
        }
    }

    // Workspace 1 <-> N Entry
    // 양방향 연관관계 편의 메서드
    public void addEntry(Entry entry) {
        this.entries.add(entry);

        if (entry.getWorkspace() != this) {
            entry.assignWorkspace(this);
        }
    }
}
