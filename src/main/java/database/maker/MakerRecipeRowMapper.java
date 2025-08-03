package database.maker;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MakerRecipeRowMapper implements RowMapper<MakerRecipe> {

    @Override
    public MakerRecipe map(ResultSet rs, StatementContext ctx) throws SQLException {
        int itemId = rs.getInt("item_id");
        short jobCategory = rs.getShort("job_category");
        short requiredLevel = rs.getShort("required_level");
        short requiredMakerLevel = rs.getShort("required_maker_level");
        int mesoCost = rs.getInt("meso_cost");
        Integer requiredItem = rs.getObject("required_item", Integer.class);
        Integer requiredEquip = rs.getObject("required_equip", Integer.class);
        Integer catalyst = rs.getObject("catalyst", Integer.class);
        short quantity = rs.getShort("quantity");
        short reagentSlots = rs.getShort("reagent_slots");
        return new MakerRecipe(itemId, jobCategory, requiredLevel, requiredMakerLevel, mesoCost, requiredItem,
                requiredEquip, catalyst, quantity, reagentSlots);
    }
}
