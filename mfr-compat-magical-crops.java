package mechaet.mfr.compat.magicalcrops;


import java.lang.reflect.Method;


import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.world.World;
import powercrystals.minefactoryreloaded.MFRRegistry;
import powercrystals.minefactoryreloaded.MineFactoryReloadedCore;
import powercrystals.minefactoryreloaded.api.HarvestType;
import powercrystals.minefactoryreloaded.farmables.plantables.PlantableStandard;
import powercrystals.minefactoryreloaded.farmables.fertilizables.FertilizableSapling;
import powercrystals.minefactoryreloaded.farmables.harvestables.HarvestableStandard;
import powercrystals.minefactoryreloaded.farmables.harvestables.HarvestableTreeLeaves;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.registry.GameData;
import cpw.mods.fml.common.registry.GameRegistry;


@Mod(modid = "MineFactoryReloaded|CompatMagicalCrops", name = "MFR Compat: MagicalCrops", version = 0.1a, 
	 dependencies = "after:MineFactoryReloaded;after:magicalcrops")
@NetworkMod(clientSideRequired = false, serverSideRequired = false)
public class MagicalCropsMFRCompat
{
	@Instance(value = "MFRCompatMagicalCrops")
    public static MagicalCropsMFRCompat instance;
	
	private static final Logger _log;
	static {
		//Init the logger
		_log = Logger.getLogger("mechaet.mfr.compat.magicalcrops.MagicalCropsMFRCompat");
		_log.setParent(FMLLog.getLogger());
	}
	@EventHandler
	public void preInit(FMLPreInitializationEvent event)
	{
		instance = this;
	}
	private void registerMfr() throws Exception
	{
		//I enumerate the materials to increase the chances of working with future versions
		//but at the cost of not automatically working with new plants.
		
		//Registering materials
		String[] mMaterials =
			{
			"Alumin",
			"Blaze",
			"Coal",
			"Copper",
			"Diamond",
			"Dye",
			"Emerald",
			"Ender",
			"Glowstone",
			"Gold",
			"Iron",
			"Lapis",
			"Lead",
			"Nether",
			"Obsidian",
			"Peridot",
			"Quartz",
			"Redstone",
			"Ruby",
			"Sapphire",
			"Silver",
			"Tin",
			};
		registerMfr(mMaterials, "m");
		
		//Registering elements
		String[] eMaterials =
			{
				"Water",
				"Fire",
				"Earth",
				"Air",
			};
		registerMfr(eMaterials, "e");
		
		//Registering your soul
		String[] soulMaterials =
			{
				"Cow",
				"Creeper",
				"Skeleton",
				"Slime",
				"Skeleton",
				"Spider",
				"Ghast",
			};
		registerMfr(soulMaterials, "soul");
	}
	private void registerMfr(String[] materials, String prefix) throws Exception
	{
	    Constructor plantableconstructor = null;
	    Constructor harvestconstructor = null;
		
	    //Yes, it is wasteful to do this reflection once for each type, but it should only really
	    //add miliseconds.
		try {
			//Planter
			Class plantableClass = Class.forName("powercrystals.minefactoryreloaded.farmables.plantables.PlantableCropPlant");
			
		    Constructor[] plantableAllConstructors = plantableClass.getDeclaredConstructors();
		    for (Constructor ctor : plantableAllConstructors) {
		    	Class<?>[] pType  = ctor.getParameterTypes();
		    	
		    	if( pType.length != 2  ||  pType[0] != int.class  ||  pType[1] != int.class )
		    	{
		    		continue;
		    	}
		    	plantableconstructor = ctor;
		    }
		    
		    //Harvester
			Class harvestClass = Class.forName("powercrystals.minefactoryreloaded.farmables.harvestables.HarvestableCropPlant");
			
		    Constructor[] harvestAllConstructors = harvestClass.getDeclaredConstructors();
		    for (Constructor ctor : harvestAllConstructors) {
		    	Class<?>[] pType  = ctor.getParameterTypes();
		    	
		    	if( pType.length != 2  ||  pType[0] != int.class  ||  pType[1] != int.class )
		    	{
		    		continue;
		    	}
		    	harvestconstructor = ctor;
		    }

		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Failed to init mfr Planter/Harvester classes",e);
		}
		
		//Base class of the MC mod
		Class cropsModClass = Class.forName("magicalcrops.mod_mCrops");
		
		//Loop through all the crops that were passed in and add them to the list.
		for( String material : materials )
		{
			//Shared
			String cropName = prefix+"Crop"+material;
			String seedName = prefix.charAt(0)+"Seeds"+material;
			
			Block crop = (Block)cropsModClass.getField(cropName).get(null);
			Item seed = (Item) cropsModClass.getField(seedName).get(null);
			
			if( crop == null  || seed == null )
			{
				_log.warning("Unable to find crop "+cropName+" by reflection.  Maybe that crop is disabled or something?");
				continue;
			}
			
			//Planter
			if( !(seed instanceof IPlantable) )
			{
				throw new IllegalArgumentException("Seed "+seed.getUnlocalizedName()+" is not IPlantable.  Type is "+seed.getClass());
			};
			
			//Using MFR code to register for Planter
			MFRRegistry.registerPlantable(new PlantableStandard(seed.itemID, crop.blockID));
			//IFactoryPlantable plantable = (IFactoryPlantable) plantableconstructor.newInstance(seed.itemID, crop.blockID);
			//FactoryRegistry.registerPlantable(plantable);
			
			////Using MFR code to register for Harvester
			MFRRegistry.registerHarvestable(new HarvestableCropPlant(crop.blockID,7));
			//IFactoryHarvestable harvestable = (IFactoryHarvestable) harvestconstructor.newInstance(crop.blockID,7);
			//FactoryRegistry.registerHarvestable(harvestable);
			
			_log.finer("Registered crop "+cropName+" with the MFR planter and harvester.");
		}
	}
	public static void postInit(FMLPostInitializationEvent e)
	{
		if(!Loader.isModLoaded("magicalcrops"))
		{
			FMLLog.warning("magicalcrops missing - MFR Magical Crops Compat not loading");
			return;
		}
		try
		{
			_log.log(Level.INFO, "Loading Magical Crops for MFR");
			//Doing this in post-init to minimize problems with Magical Crops
			//not having finished init yet.
			registerMfr();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
}

