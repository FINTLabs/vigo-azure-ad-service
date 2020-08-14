package no.vigo.provisioning.qlik;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class QlikUserMapper implements RowMapper<QlikUser> {
    @Override
    public QlikUser mapRow(ResultSet rs, int rowNum) throws SQLException {
        return QlikUser.builder()
                .countyNumber(rs.getString(CBrukerFields.FYLKESNR))
                .firstName(rs.getString(CBrukerFields.FORNAVN))
                .lastName(rs.getString(CBrukerFields.ETTERNAVN))
                .email(rs.getString(CBrukerFields.EPOST))
                .mobile(rs.getString(CBrukerFields.MOBIL))
                .qlikSenseDeveloper(rs.getString(CBrukerFields.QLIK_SENSE_DEVELOPER).equals("J"))
                .qlikSenseRead(rs.getString(CBrukerFields.QLIK_SENSE_READ).equals("J"))
                .qlikViewDeveloper(rs.getString(CBrukerFields.QLIK_VIEW_DEVELOPER).equals("J"))
                .qlikViewRead(rs.getString(CBrukerFields.QLIK_VIEW_READ).equals("J"))
                .nPrintDeveloper(rs.getString(CBrukerFields.NPRINT_DEVELOPER).equals("J"))
                .nPrintRead(rs.getString(CBrukerFields.NPRINT_READ).equals("J"))
                .build();
    }
}
