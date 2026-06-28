package id.aqilaadam.wisatasuara;

import org.json.JSONException;
import org.json.JSONObject;

public class Artefak {
    private String id;
    private String nama;
    private String sejarah;
    private String deskripsiVisual;

    public static Artefak fromJsonObject(JSONObject obj) throws JSONException {
        Artefak artefak = new Artefak();
        if (obj.has("id")) artefak.id = obj.getString("id");
        if (obj.has("nama")) artefak.nama = obj.getString("nama");
        if (obj.has("sejarah")) artefak.sejarah = obj.getString("sejarah");
        if (obj.has("deskripsiVisual")) artefak.deskripsiVisual = obj.getString("deskripsiVisual");
        return artefak;
    }

    public String getId() { return id != null ? id : ""; }
    public String getNama() { return nama != null ? nama : ""; }
    public String getSejarah() { return sejarah != null ? sejarah : ""; }
    public String getDeskripsiVisual() { return deskripsiVisual != null ? deskripsiVisual : ""; }
}