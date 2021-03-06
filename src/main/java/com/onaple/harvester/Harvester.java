package com.onaple.harvester;

import javax.inject.Inject;

import com.onaple.harvester.command.ReloadCommand;
import com.onaple.harvester.data.dao.RespawningBlockDao;
import com.onaple.harvester.data.handlers.ConfigurationHandler;
import com.onaple.harvester.exception.PluginNotFoundException;
import com.onaple.harvester.utils.SpawnUtil;
import com.onaple.itemizer.service.IItemService;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.asset.Asset;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Plugin(id = "harvester", name = "Harvester", version = "1.0.0")
public class Harvester {

	@Inject
	@ConfigDir(sharedRoot=true)
	private Path configDir;

	private static Logger logger;
	@Inject
	private void setLogger(Logger logger) {
		Harvester.logger = logger;
	}
	public static Logger getLogger() {
		return logger;
	}

	private static Harvester harvester;

	public static Harvester getHarvester() {
		return harvester;
	}

	private static GlobalConfiguration globalConfiguration;

	public static GlobalConfiguration getGlobalConfiguration(){return globalConfiguration;}

	public PluginContainer getInstance() throws PluginNotFoundException {
			return Sponge.getPluginManager().getPlugin("harvester").orElseThrow(() ->  new PluginNotFoundException("harvester"));
	}

	private static Optional<IItemService> itemService;
	public static Optional<IItemService> getItemService(){ return itemService;}

	@Listener
	public void onServerStart(GameInitializationEvent event) {

		harvester = this;
		if(Sponge.getPluginManager().getPlugin("itemizer").isPresent()) {
			Optional itemServiceOptional = Sponge.getServiceManager().provide(IItemService.class);
			if (itemServiceOptional.isPresent()) {
				itemService = itemServiceOptional;
				logger.info("Itemizer dependency found!");
			} else {
				logger.warn("Itemizer dependency not found");
			}
		}

		Sponge.getEventManager().registerListeners(this, new HarvestListener());
		RespawningBlockDao.createTableIfNotExist();

		/* load block configuration*/
		try {
			logger.info("{} of Block in configuration", loadHarvestable());
		} catch (IOException | ObjectMappingException e) {
			logger.error("Error while reading harvestable configuration: ", e);
		}

		/* load drops configuration */
		try {
			logger.info("{} of drops in configuration", loadDrops());

		} catch (IOException | ObjectMappingException e) {
			logger.error("Error while reading drop configuration: ", e);
		}
		/* load Global configuration */
		try {
			loadGlobal();
			logger.info("Loaded global configuration");
			logger.info("Enabled Harvester into {} worlds.", globalConfiguration.getWorldNames().size());
			logger.info("Command run when blocks break : {}", globalConfiguration.getBlockBreakCommand());
		} catch (IOException | ObjectMappingException e) {
			logger.error("Error while reading the global configuration", e);
		}

		Task.builder().execute(SpawnUtil::checkBlockRespawn)
                .delay(5, TimeUnit.SECONDS).interval(30, TimeUnit.SECONDS)
                .name("Task respawning mined resources.").submit(this);

		CommandSpec reloadCommand = CommandSpec.builder()
				.description(Text.of("Reload Harvester configuration from files."))
				.permission("harvester.command.reload")
				.executor(new ReloadCommand()).build();

		CommandSpec harvesterCommand = CommandSpec.builder()
				.description(Text.of("Harvester utility commands"))
				.permission("harvester.command")
				.child(reloadCommand, "reload")
				.build();

		Sponge.getCommandManager().register(this, harvesterCommand, "harvester");

        logger.info("HARVESTER initialized.");
	}

	/**
	 * Load Harvester configuration. If the config file doesn't exist, it loads the default file
	 * @return the number of harvestable block imported
	 * @throws IOException error when copying default config in config/harvester/ folder
	 * @throws ObjectMappingException error when the configuration file have an syntax error
	 */
	public int loadHarvestable() throws IOException, ObjectMappingException {
		initDefaultConfig("harvestables.conf");
		return ConfigurationHandler.readHarvestablesConfiguration(ConfigurationHandler.loadConfiguration(configDir+"/harvester/harvestables.conf"));
	}
	/**
	 * Load drops configuration. If the config file does'nt exist, it load the default file
	 * @return the number of drops imported
	 * @throws IOException error when copying default config in config/harvester/ folder
	 * @throws ObjectMappingException error when the configuration file have an syntax error
	 */
	public int loadDrops() throws IOException, ObjectMappingException {
		initDefaultConfig("drops.conf");
		return ConfigurationHandler.readHarvestDropsConfiguration(ConfigurationHandler.loadConfiguration(configDir+"/harvester/drops.conf"));
	}
	public void loadGlobal() throws IOException, ObjectMappingException {
		initDefaultConfig("global.conf");
		Harvester.globalConfiguration = ConfigurationHandler.readGlobalConfiguration(
				ConfigurationHandler.loadConfiguration(configDir+"/harvester/global.conf"));
	}

	/**
	 * Load the default configuration from resources if no files found.
	 * @param path
	 */
	public void initDefaultConfig(String path){
		if (Files.notExists(Paths.get(configDir +"/harvester/" + path))) {
			try {
				PluginContainer pluginInstance = getInstance();
				Optional<Asset> defaultConfigFile = pluginInstance.getAsset(path);
				getLogger().info("No config file set for " + path + ". Default config will be loaded.");
				if (defaultConfigFile.isPresent()) {
					try {
						defaultConfigFile.get().copyToDirectory(Paths.get(configDir + "/harvester/"));
					} catch (IOException e) {
						getLogger().error("Error while setting default configuration : " + e.getMessage());
					}
				} else {
					logger.warn("Default config not found!");
				}

			}
			catch (PluginNotFoundException e){
				getLogger().error(e.toString());
			}
		}
	}
}
